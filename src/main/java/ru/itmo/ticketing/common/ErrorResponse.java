package ru.itmo.ticketing.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        OffsetDateTime timestamp
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of(), OffsetDateTime.now());
    }
}
