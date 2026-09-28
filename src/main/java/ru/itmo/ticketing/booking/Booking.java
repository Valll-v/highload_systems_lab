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
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.user.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(User customer, Event event, BigDecimal totalPrice) {
        this.customer = customer;
        this.event = event;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
    }

    public Event getEvent() {
        return event;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public void cancel(OffsetDateTime at) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = at;
    }
}
