package ru.itmo.ticketing.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @NotNull
    @Column(nullable = false)
    private Long customerId;

    @NotNull
    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String eventTitle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime cancelledAt;

    public Booking(Long customerId, Long eventId, String eventTitle, BigDecimal totalPrice) {
        this.customerId = customerId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
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
