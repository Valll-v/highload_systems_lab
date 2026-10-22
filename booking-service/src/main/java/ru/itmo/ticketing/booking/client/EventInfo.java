package ru.itmo.ticketing.booking.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Часть ответа event-service на GET /api/events/{id}, остальные поля не читаются.
 */
public record EventInfo(
        Long id,
        String title,
        Long organizerId,
        Long hallId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal price,
        EventStatus status
) {
}
