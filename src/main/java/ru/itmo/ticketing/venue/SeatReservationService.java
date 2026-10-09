package ru.itmo.ticketing.venue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.NotFoundException;
import ru.itmo.ticketing.event.Event;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatRepository seats;
    private final SeatReservationRepository reservations;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Seat> reserve(Event event, Collection<Long> seatIds, Booking booking) {
        Set<Long> requested = new LinkedHashSet<>(seatIds);
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }
        List<Seat> locked = seats.lockAllByIdIn(requested);
        if (locked.size() != requested.size()) {
            Set<Long> found = new HashSet<>(locked.stream().map(Seat::getId).toList());
            requested.removeAll(found);
            throw new NotFoundException("Seats not found: " + requested);
        }
        Long hallId = event.getHall().getId();
        for (Seat seat : locked) {
            if (!seat.getHall().getId().equals(hallId)) {
                throw new ConflictException("Seat " + seat.getId() + " does not belong to the event's hall");
            }
        }
        List<Long> taken = reservations.findReservedSeatIds(event.getId(), requested);
        if (!taken.isEmpty()) {
            throw new ConflictException("Seats already taken: " + taken);
        }
        for (Seat seat : locked) {
            reservations.save(new SeatReservation(event, seat, booking));
        }
        reservations.flush();
        return locked;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Booking booking) {
        reservations.deleteByBookingId(booking.getId());
    }

    @Transactional(readOnly = true)
    public Set<Long> reservedSeatIds(Long eventId) {
        return new HashSet<>(reservations.findReservedSeatIds(eventId));
    }

    @Transactional(readOnly = true)
    public long reservedCount(Long eventId) {
        return reservations.countByEventId(eventId);
    }
}
