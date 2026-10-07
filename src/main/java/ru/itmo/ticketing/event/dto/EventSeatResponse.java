package ru.itmo.ticketing.event.dto;

public record EventSeatResponse(Long id, int row, int number, boolean available) {
}
