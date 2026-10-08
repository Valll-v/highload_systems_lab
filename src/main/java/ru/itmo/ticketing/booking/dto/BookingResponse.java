package ru.itmo.ticketing.booking.dto;

import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.booking.BookingStatus;
import ru.itmo.ticketing.ticket.Ticket;
import ru.itmo.ticketing.ticket.dto.TicketResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long customerId,
        Long eventId,
        String eventTitle,
        BookingStatus status,
        BigDecimal totalPrice,
        OffsetDateTime createdAt,
        OffsetDateTime cancelledAt,
        List<TicketResponse> tickets
) {
    public static BookingResponse from(Booking b, List<Ticket> tickets) {
        return new BookingResponse(
                b.getId(), b.getCustomer().getId(), b.getEvent().getId(), b.getEvent().getTitle(),
                b.getStatus(), b.getTotalPrice(), b.getCreatedAt(), b.getCancelledAt(),
                tickets.stream().map(TicketResponse::from).toList());
    }
}
