package ru.itmo.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Пара (мероприятие, место) с уникальным индексом: одно место на мероприятие
 * продаётся только раз, даже при параллельных запросах.
 */
@Entity
@Table(name = "seat_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long eventId;

    @NotNull
    @Column(nullable = false)
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Booking booking;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public SeatReservation(Long eventId, Long seatId, Booking booking) {
        this.eventId = eventId;
        this.seatId = seatId;
        this.booking = booking;
    }
}
