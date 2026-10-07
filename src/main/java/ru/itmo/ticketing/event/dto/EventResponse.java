package ru.itmo.ticketing.event.dto;

import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        Long categoryId,
        String category,
        Long organizerId,
        Long hallId,
        String hall,
        Long venueId,
        String venue,
        String city,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal price,
        EventStatus status
) {
    public static EventResponse from(Event e) {
        var hall = e.getHall();
        var venue = hall.getVenue();
        return new EventResponse(
                e.getId(), e.getTitle(), e.getDescription(),
                e.getCategory().getId(), e.getCategory().getName(),
                e.getOrganizer().getId(),
                hall.getId(), hall.getName(),
                venue.getId(), venue.getName(), venue.getCity(),
                e.getStartsAt(), e.getEndsAt(), e.getPrice(), e.getStatus());
    }
}
