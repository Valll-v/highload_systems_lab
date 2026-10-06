package ru.itmo.ticketing.venue.dto;

import ru.itmo.ticketing.venue.Seat;

public record SeatResponse(Long id, int row, int number) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowNo(), seat.getSeatNo());
    }
}
