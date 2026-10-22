package ru.itmo.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.booking.client.EventClient;
import ru.itmo.ticketing.booking.client.EventInfo;
import ru.itmo.ticketing.booking.client.SeatInfo;
import ru.itmo.ticketing.booking.client.SeatPage;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.common.ForbiddenException;
import ru.itmo.ticketing.booking.common.Paging;
import ru.itmo.ticketing.booking.common.SliceResponse;
import ru.itmo.ticketing.booking.dto.EventSeatResponse;
import ru.itmo.ticketing.booking.dto.SalesReport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Схема мест с занятостью и отчёт по продажам: зал из event-service, занятость из своей базы.
 */
@Service
@RequiredArgsConstructor
public class EventStatsService {

    private final SeatReservationRepository reservations;
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final EventClient events;

    @Transactional(readOnly = true)
    public SliceResponse<EventSeatResponse> seatMap(Long eventId, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size >= 1");
        }
        EventInfo event = events.event(eventId);
        SeatPage seats = events.seats(event.hallId(), page, Math.min(size, Paging.MAX_SIZE));
        List<Long> ids = seats.content().stream().map(SeatInfo::id).toList();
        Set<Long> taken = ids.isEmpty() ? Set.of()
                : Set.copyOf(reservations.findReservedSeatIds(eventId, ids));
        List<EventSeatResponse> content = seats.content().stream()
                .map(s -> new EventSeatResponse(s.id(), s.row(), s.number(), !taken.contains(s.id())))
                .collect(Collectors.toList());
        return new SliceResponse<>(content, seats.page(), seats.size(), seats.hasNext());
    }

    @Transactional(readOnly = true)
    public SalesReport sales(UserInfo actor, Long eventId) {
        EventInfo event = events.eventAs(eventId, actor.id());
        if (!actor.isAdmin() && !actor.id().equals(event.organizerId())) {
            throw new ForbiddenException("Sales report is available to the event organizer only");
        }
        int capacity = events.hall(event.hallId()).capacity();
        return new SalesReport(
                eventId, capacity,
                tickets.countByEventIdAndStatus(eventId, TicketStatus.VALID),
                tickets.countByEventIdAndStatus(eventId, TicketStatus.CANCELLED),
                capacity - reservations.countByEventId(eventId),
                bookings.countByEventIdAndStatus(eventId, BookingStatus.ACTIVE),
                tickets.sumPriceByEventIdAndStatus(eventId, TicketStatus.VALID));
    }
}
