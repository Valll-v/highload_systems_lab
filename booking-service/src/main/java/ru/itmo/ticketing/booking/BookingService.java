package ru.itmo.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.ticketing.booking.client.EventClient;
import ru.itmo.ticketing.booking.client.EventInfo;
import ru.itmo.ticketing.booking.client.EventStatus;
import ru.itmo.ticketing.booking.client.SeatInfo;
import ru.itmo.ticketing.booking.client.SeatPage;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.common.ConflictException;
import ru.itmo.ticketing.booking.common.ForbiddenException;
import ru.itmo.ticketing.booking.common.NotFoundException;
import ru.itmo.ticketing.booking.dto.BookingResponse;
import ru.itmo.ticketing.booking.dto.CancelledBookings;
import ru.itmo.ticketing.booking.dto.TicketResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Блокирующий сервис: JPA и Feign. Контроллер зовёт его через Blocking.call.
 * Вызовы в другие сервисы делаются до открытия транзакции, чтобы не держать
 * соединение с базой на время HTTP.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int SEAT_PAGE = 50;

    private final BookingRepository bookings;
    private final SeatReservationRepository reservations;
    private final TicketRepository tickets;
    private final EventClient events;
    private final TransactionTemplate tx;

    public BookingResponse create(UserInfo customer, Long eventId, List<Long> seatIds) {
        Set<Long> unique = new LinkedHashSet<>(seatIds);
        if (unique.size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat ids in request");
        }
        EventInfo event = events.eventAs(eventId, customer.id());
        if (event.status() != EventStatus.PUBLISHED) {
            throw new ConflictException("Event is not open for booking (status " + event.status() + ")");
        }
        if (!event.startsAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Event has already started");
        }
        Map<Long, SeatInfo> seats = hallSeats(event.hallId(), unique);

        return tx.execute(status -> {
            List<Long> taken = reservations.findReservedSeatIds(eventId, unique);
            if (!taken.isEmpty()) {
                throw new ConflictException("Seats already taken: " + taken);
            }
            BigDecimal total = event.price().multiply(BigDecimal.valueOf(unique.size()));
            Booking booking = bookings.save(new Booking(customer.id(), eventId, event.title(), total));
            List<Ticket> issued = new ArrayList<>();
            for (Long seatId : unique) {
                SeatInfo seat = seats.get(seatId);
                reservations.save(new SeatReservation(eventId, seatId, booking));
                issued.add(new Ticket(booking, eventId, seatId, seat.row(), seat.number(), newCode(), event.price()));
            }
            reservations.flush();
            return BookingResponse.from(booking, tickets.saveAll(issued));
        });
    }

    @Transactional
    public BookingResponse cancel(UserInfo actor, Long bookingId) {
        Booking booking = getAccessible(actor, bookingId);
        if (!booking.isActive()) {
            throw new ConflictException("Booking is already cancelled");
        }
        cancelInternal(booking);
        return toResponse(booking);
    }

    /**
     * Зовётся event-service после отмены мероприятия. Повторный вызов безопасен.
     */
    public CancelledBookings cancelByEvent(UserInfo actor, Long eventId) {
        EventInfo event = events.eventAs(eventId, actor.id());
        if (event.status() != EventStatus.CANCELLED) {
            throw new ConflictException("Event " + eventId + " is not cancelled, bookings stay active");
        }
        if (!actor.isAdmin() && !actor.id().equals(event.organizerId())) {
            throw new ForbiddenException("Only the organizer or an admin can cancel bookings of the event");
        }
        return tx.execute(status -> {
            List<Booking> active = bookings.findAllByEventIdAndStatus(eventId, BookingStatus.ACTIVE);
            active.forEach(this::cancelInternal);
            return new CancelledBookings(eventId, active.size());
        });
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UserInfo actor, Long bookingId) {
        return toResponse(getAccessible(actor, bookingId));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listForCustomer(UserInfo customer, Pageable pageable) {
        return bookings.findAllByCustomerId(customer.id(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> ticketsForCustomer(UserInfo customer, Pageable pageable) {
        return tickets.findAllByBookingCustomerId(customer.id(), pageable).map(TicketResponse::from);
    }

    private Map<Long, SeatInfo> hallSeats(Long hallId, Set<Long> wanted) {
        Map<Long, SeatInfo> found = new HashMap<>();
        int page = 0;
        SeatPage seats;
        do {
            seats = events.seats(hallId, page++, SEAT_PAGE);
            for (SeatInfo seat : seats.content()) {
                if (wanted.contains(seat.id())) {
                    found.put(seat.id(), seat);
                }
            }
        } while (found.size() < wanted.size() && seats.hasNext());
        if (found.size() != wanted.size()) {
            Set<Long> missing = new LinkedHashSet<>(wanted);
            missing.removeAll(found.keySet());
            throw new ConflictException("Seats " + missing + " do not belong to the event's hall");
        }
        return found;
    }

    private Booking getAccessible(UserInfo actor, Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
        if (!booking.getCustomerId().equals(actor.id()) && !actor.isAdmin()) {
            throw new ForbiddenException("Booking " + bookingId + " belongs to another customer");
        }
        return booking;
    }

    private void cancelInternal(Booking booking) {
        booking.cancel(OffsetDateTime.now());
        tickets.findAllByBookingIdOrderByIdAsc(booking.getId()).forEach(Ticket::cancel);
        reservations.deleteByBookingId(booking.getId());
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.from(booking, tickets.findAllByBookingIdOrderByIdAsc(booking.getId()));
    }

    private static String newCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
