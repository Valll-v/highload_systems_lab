package ru.itmo.ticketing.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.ticketing.booking.common.NotFoundException;
import ru.itmo.ticketing.booking.common.ServiceUnavailableException;

/**
 * 404 от event-service пробрасывается как есть, остальные ошибки и открытый
 * circuit breaker превращаются в 503.
 */
@Slf4j
@Component
public class EventClientFallback implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable cause) {
        return new EventClient() {
            @Override
            public EventInfo event(long id) {
                throw translate("event " + id);
            }

            @Override
            public EventInfo eventAs(long id, long userId) {
                throw translate("event " + id);
            }

            @Override
            public HallInfo hall(long id) {
                throw translate("hall " + id);
            }

            @Override
            public SeatPage seats(long hallId, int page, int size) {
                throw translate("seats of hall " + hallId);
            }

            private RuntimeException translate(String what) {
                if (cause instanceof NotFoundException notFound) {
                    return notFound;
                }
                log.warn("event-service call failed for {}: {}", what, cause.toString());
                return new ServiceUnavailableException("event-service is unavailable");
            }
        };
    }
}
