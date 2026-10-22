package ru.itmo.ticketing.booking.client;

import java.util.List;

/**
 * Страница схемы зала из event-service: формат бесконечной прокрутки, без общего количества.
 */
public record SeatPage(List<SeatInfo> content, int page, int size, boolean hasNext) {
}
