package ru.itmo.ticketing.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.ticketing.booking.common.ServiceUnavailableException;
import ru.itmo.ticketing.booking.common.UnauthorizedException;

@Slf4j
@Component
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return userId -> {
            if (cause instanceof UnauthorizedException unauthorized) {
                throw unauthorized;
            }
            log.warn("user-service call failed for user {}: {}", userId, cause.toString());
            throw new ServiceUnavailableException("user-service is unavailable");
        };
    }
}
