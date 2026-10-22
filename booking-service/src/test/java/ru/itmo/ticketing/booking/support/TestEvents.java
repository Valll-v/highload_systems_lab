package ru.itmo.ticketing.booking.support;

import org.springframework.stereotype.Component;
import ru.itmo.ticketing.booking.client.EventInfo;
import ru.itmo.ticketing.booking.client.EventStatus;
import ru.itmo.ticketing.booking.client.HallInfo;
import ru.itmo.ticketing.booking.client.SeatInfo;
import ru.itmo.ticketing.booking.client.SeatPage;
import ru.itmo.ticketing.booking.common.NotFoundException;
import ru.itmo.ticketing.booking.common.ServiceUnavailableException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Мероприятия и залы для тестов вместо event-service. Видимость как у настоящего:
 * аноним видит только PUBLISHED, организатор и админ видят всё.
 */
@Component
public class TestEvents {

    private final Map<Long, HallInfo> halls = new ConcurrentHashMap<>();
    private final Map<Long, List<SeatInfo>> hallSeats = new ConcurrentHashMap<>();
    private final Map<Long, EventInfo> events = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean down;

    public long hall(int rows, int seatsPerRow) {
        long id = ids.incrementAndGet();
        List<SeatInfo> seats = new ArrayList<>();
        for (int r = 1; r <= rows; r++) {
            for (int n = 1; n <= seatsPerRow; n++) {
                seats.add(new SeatInfo(ids.incrementAndGet(), r, n));
            }
        }
        halls.put(id, new HallInfo(id, 1L, "Hall " + id, seats.size()));
        hallSeats.put(id, seats);
        return id;
    }

    public List<Long> seatIds(long hallId) {
        return hallSeats.get(hallId).stream().map(SeatInfo::id).toList();
    }

    public long event(long organizerId, long hallId, EventStatus status, BigDecimal price) {
        long id = ids.incrementAndGet();
        OffsetDateTime start = OffsetDateTime.now().plusDays(7);
        events.put(id, new EventInfo(id, "Event " + id, organizerId, hallId, start, start.plusHours(2), price, status));
        return id;
    }

    public void status(long eventId, EventStatus status) {
        EventInfo e = events.get(eventId);
        events.put(eventId, new EventInfo(e.id(), e.title(), e.organizerId(), e.hallId(), e.startsAt(), e.endsAt(), e.price(), status));
    }

    public void down(boolean down) {
        this.down = down;
    }

    public EventInfo event(long id, Long viewerId, TestUsers users) {
        check();
        EventInfo event = events.get(id);
        if (event == null) {
            throw new NotFoundException("Event", id);
        }
        if (event.status() == EventStatus.PUBLISHED) {
            return event;
        }
        var viewer = viewerId == null ? null : users.get(viewerId);
        if (viewer != null && (viewer.isAdmin() || viewer.id().equals(event.organizerId()))) {
            return event;
        }
        throw new NotFoundException("Event", id);
    }

    public HallInfo hall(long id) {
        check();
        HallInfo hall = halls.get(id);
        if (hall == null) {
            throw new NotFoundException("Hall", id);
        }
        return hall;
    }

    public SeatPage seats(long hallId, int page, int size) {
        check();
        List<SeatInfo> all = hallSeats.get(hallId);
        if (all == null) {
            throw new NotFoundException("Hall", hallId);
        }
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new SeatPage(all.subList(from, to), page, size, to < all.size());
    }

    private void check() {
        if (down) {
            throw new ServiceUnavailableException("event-service is unavailable");
        }
    }
}
