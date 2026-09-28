package ru.itmo.ticketing.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.event.Event;

import java.time.OffsetDateTime;

@Entity
@Table(name = "seat_reservations")
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SeatReservation() {
    }

    public SeatReservation(Event event, Seat seat, Booking booking) {
        this.event = event;
        this.seat = seat;
        this.booking = booking;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Seat getSeat() {
        return seat;
    }

    public Booking getBooking() {
        return booking;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
