package ru.itmo.ticketing.booking.dto;

public record EventSeatResponse(Long id, int row, int number, boolean available) {
}
