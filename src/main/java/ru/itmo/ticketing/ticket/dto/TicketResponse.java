package ru.itmo.ticketing.ticket.dto;

import ru.itmo.ticketing.ticket.Ticket;
import ru.itmo.ticketing.ticket.TicketStatus;

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
                t.getId(), t.getCode(), t.getBooking().getId(), t.getEvent().getId(),
                t.getSeat().getId(), t.getSeat().getRowNo(), t.getSeat().getSeatNo(),
                t.getPrice(), t.getStatus(), t.getIssuedAt());
    }
}
