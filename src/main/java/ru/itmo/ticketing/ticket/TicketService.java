package ru.itmo.ticketing.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.ticket.dto.TicketResponse;
import ru.itmo.ticketing.venue.Seat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository tickets;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Ticket> issue(Booking booking, Event event, List<Seat> seats) {
        List<Ticket> issued = new ArrayList<>(seats.size());
        for (Seat seat : seats) {
            issued.add(new Ticket(booking, event, seat, newCode(), event.getPrice()));
        }
        return tickets.saveAll(issued);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelAll(Booking booking) {
        tickets.findAllByBookingIdOrderByIdAsc(booking.getId()).forEach(Ticket::cancel);
    }

    @Transactional(readOnly = true)
    public List<Ticket> forBooking(Long bookingId) {
        return tickets.findAllByBookingIdOrderByIdAsc(bookingId);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> forCustomer(Long customerId) {
        return tickets.findAllByBookingCustomerIdOrderByIdDesc(customerId).stream()
                .map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countValid(Long eventId) {
        return tickets.countByEventIdAndStatus(eventId, TicketStatus.VALID);
    }

    @Transactional(readOnly = true)
    public long countCancelled(Long eventId) {
        return tickets.countByEventIdAndStatus(eventId, TicketStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public BigDecimal revenue(Long eventId) {
        return tickets.sumPriceByEventIdAndStatus(eventId, TicketStatus.VALID);
    }

    private static String newCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
