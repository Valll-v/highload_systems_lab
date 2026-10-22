package ru.itmo.ticketing.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.ticketing.common.ServiceUnavailableException;
import ru.itmo.ticketing.common.UnauthorizedException;

/**
 * Срабатывает на любую ошибку вызова и на открытый circuit breaker.
 * Неизвестный пользователь пробрасывается как 401, всё остальное превращается в 503.
 */
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
