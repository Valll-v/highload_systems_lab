package ru.itmo.ticketing.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.ticketing.common.ServiceUnavailableException;

@Slf4j
@Component
public class BookingClientFallback implements FallbackFactory<BookingClient> {

    @Override
    public BookingClient create(Throwable cause) {
        return (eventId, actorId) -> {
            log.warn("booking-service call failed for event {}: {}", eventId, cause.toString());
            throw new ServiceUnavailableException("booking-service is unavailable");
        };
    }
}
