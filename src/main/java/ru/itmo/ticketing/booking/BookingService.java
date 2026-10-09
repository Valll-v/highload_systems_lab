package ru.itmo.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.booking.dto.BookingResponse;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.ForbiddenException;
import ru.itmo.ticketing.common.NotFoundException;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventCancelledEvent;
import ru.itmo.ticketing.event.EventService;
import ru.itmo.ticketing.ticket.TicketService;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Seat;
import ru.itmo.ticketing.venue.SeatReservationService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookings;
    private final EventService events;
    private final SeatReservationService reservations;
    private final TicketService tickets;

    @Transactional
    public BookingResponse create(User customer, Long eventId, List<Long> seatIds) {
        Set<Long> uniqueSeats = new LinkedHashSet<>(seatIds);
        if (uniqueSeats.size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat ids in request");
        }

        Event event = events.getBookable(eventId);

        BigDecimal total = event.getPrice().multiply(BigDecimal.valueOf(uniqueSeats.size()));
        Booking booking = bookings.save(new Booking(customer, event, total));

        List<Seat> seats = reservations.reserve(event, uniqueSeats, booking);

        tickets.issue(booking, event, seats);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(User actor, Long bookingId) {
        Booking booking = getAccessible(actor, bookingId);
        if (!booking.isActive()) {
            throw new ConflictException("Booking is already cancelled");
        }
        cancelInternal(booking);
        return toResponse(booking);
    }

    @EventListener
    @Transactional
    public void onEventCancelled(EventCancelledEvent cancelled) {
        bookings.findAllByEventIdAndStatus(cancelled.eventId(), BookingStatus.ACTIVE)
                .forEach(this::cancelInternal);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(User actor, Long bookingId) {
        return toResponse(getAccessible(actor, bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForCustomer(User customer) {
        return bookings.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countActive(Long eventId) {
        return bookings.countByEventIdAndStatus(eventId, BookingStatus.ACTIVE);
    }

    private Booking getAccessible(User actor, Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
        boolean owner = booking.getCustomer().getId().equals(actor.getId());
        if (!owner && actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Booking " + bookingId + " belongs to another customer");
        }
        return booking;
    }

    private void cancelInternal(Booking booking) {
        booking.cancel(OffsetDateTime.now());
        tickets.cancelAll(booking);
        reservations.release(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.from(booking, tickets.forBooking(booking.getId()));
    }
}
