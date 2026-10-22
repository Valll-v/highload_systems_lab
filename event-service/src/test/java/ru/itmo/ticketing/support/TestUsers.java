package ru.itmo.ticketing.support;

import org.springframework.stereotype.Component;
import ru.itmo.ticketing.common.ServiceUnavailableException;
import ru.itmo.ticketing.common.UnauthorizedException;
import ru.itmo.ticketing.user.UserInfo;
import ru.itmo.ticketing.user.UserRole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Пользователи для интеграционных тестов. Feign-клиент заменён на мок (IntegrationTestBase),
 * который отвечает из этой карты теми же ошибками, что настоящий клиент после ErrorDecoder и fallback.
 */
@Component
public class TestUsers {

    private final Map<Long, UserInfo> users = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean down;

    public long add(UserRole role, String email) {
        long id = ids.incrementAndGet();
        users.put(id, new UserInfo(id, email, "Test " + role, role));
        return id;
    }

    public void down(boolean down) {
        this.down = down;
    }

    public UserInfo me(long userId) {
        if (down) {
            throw new ServiceUnavailableException("user-service is unavailable");
        }
        UserInfo user = users.get(userId);
        if (user == null) {
            throw new UnauthorizedException("Unknown user " + userId);
        }
        return user;
    }
}
