package ru.itmo.ticketing.venue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHallRequest(
        @NotBlank @Size(max = 255) String name,
        @Min(1) @Max(500) int rows,
        @Min(1) @Max(500) int seatsPerRow
) {
}
