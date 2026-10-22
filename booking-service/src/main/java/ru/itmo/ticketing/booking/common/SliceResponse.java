package ru.itmo.ticketing.booking.common;

import java.util.List;

public record SliceResponse<T>(List<T> content, int page, int size, boolean hasNext) {
}
