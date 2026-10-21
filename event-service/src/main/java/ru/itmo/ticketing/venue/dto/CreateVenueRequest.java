package ru.itmo.ticketing.venue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 128) String city,
        @NotBlank @Size(max = 512) String address
) {
}
