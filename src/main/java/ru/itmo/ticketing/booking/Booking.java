package ru.itmo.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.user.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime cancelledAt;

    public Booking(User customer, Event event, BigDecimal totalPrice) {
        this.customer = customer;
        this.event = event;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public void cancel(OffsetDateTime at) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = at;
    }
}
