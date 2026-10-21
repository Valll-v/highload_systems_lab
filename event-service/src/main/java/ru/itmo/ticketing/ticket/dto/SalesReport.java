package ru.itmo.ticketing.ticket.dto;

import java.math.BigDecimal;

public record SalesReport(
        Long eventId,
        int capacity,
        long ticketsSold,
        long ticketsCancelled,
        long seatsAvailable,
        long activeBookings,
        BigDecimal revenue
) {
}
