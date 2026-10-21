package ru.itmo.ticketing.venue.dto;

import ru.itmo.ticketing.venue.Venue;

import java.util.List;

public record VenueResponse(Long id, String name, String city, String address, List<HallResponse> halls) {

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(
                venue.getId(), venue.getName(), venue.getCity(), venue.getAddress(),
                venue.getHalls().stream().map(HallResponse::from).toList());
    }
}
