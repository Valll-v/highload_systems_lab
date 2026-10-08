package ru.itmo.ticketing.ticket;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.ticketing.booking.BookingService;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.ForbiddenException;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventService;
import ru.itmo.ticketing.ticket.dto.SalesReport;
import ru.itmo.ticketing.ticket.dto.TicketResponse;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.SeatReservationService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService tickets;
    private final EventService events;
    private final BookingService bookings;
    private final SeatReservationService reservations;

    public TicketController(TicketService tickets, EventService events,
                            BookingService bookings, SeatReservationService reservations) {
        this.tickets = tickets;
        this.events = events;
        this.bookings = bookings;
        this.reservations = reservations;
    }

    @GetMapping("/tickets")
    @RequireRole(UserRole.CUSTOMER)
    public List<TicketResponse> mine(@CurrentUser User customer) {
        return tickets.forCustomer(customer.getId());
    }

    @GetMapping("/events/{id}/sales")
    @RequireRole({UserRole.ORGANIZER, UserRole.ADMIN})
    @Transactional(readOnly = true)
    public SalesReport sales(@CurrentUser User actor, @PathVariable Long id) {
        Event event = events.get(id);
        if (!event.isOwnedBy(actor) && actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Sales report is available to the event organizer only");
        }
        int capacity = event.getHall().getSeats().size();
        long sold = tickets.countValid(id);
        return new SalesReport(
                id, capacity, sold, tickets.countCancelled(id),
                capacity - reservations.reservedCount(id),
                bookings.countActive(id),
                tickets.revenue(id));
    }
}
