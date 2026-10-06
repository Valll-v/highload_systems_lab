package ru.itmo.ticketing.venue.dto;

import ru.itmo.ticketing.venue.Hall;

public record HallResponse(Long id, Long venueId, String name, int capacity) {

    public static HallResponse from(Hall hall) {
        return new HallResponse(hall.getId(), hall.getVenue().getId(), hall.getName(), hall.getSeats().size());
    }
}
