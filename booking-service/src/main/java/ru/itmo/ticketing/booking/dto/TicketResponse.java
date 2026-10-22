package ru.itmo.ticketing.booking.dto;

import ru.itmo.ticketing.booking.Ticket;
import ru.itmo.ticketing.booking.TicketStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketResponse(
        Long id,
        String code,
        Long bookingId,
        Long eventId,
        Long seatId,
        int row,
        int number,
        BigDecimal price,
        TicketStatus status,
        OffsetDateTime issuedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(), t.getCode(), t.getBooking().getId(), t.getEventId(),
                t.getSeatId(), t.getRowNo(), t.getSeatNo(),
                t.getPrice(), t.getStatus(), t.getIssuedAt());
    }
}
