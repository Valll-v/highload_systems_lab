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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Booking booking;

    @NotNull
    @Column(nullable = false)
    private Long eventId;

    @NotNull
    @Column(nullable = false)
    private Long seatId;

    @Column(nullable = false)
    private int rowNo;

    @Column(nullable = false)
    private int seatNo;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime issuedAt;

    public Ticket(Booking booking, Long eventId, Long seatId, int rowNo, int seatNo, String code, BigDecimal price) {
        this.booking = booking;
        this.eventId = eventId;
        this.seatId = seatId;
        this.rowNo = rowNo;
        this.seatNo = seatNo;
        this.code = code;
        this.price = price;
        this.status = TicketStatus.VALID;
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }
}
