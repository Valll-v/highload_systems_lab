package ru.itmo.ticketing.booking.support;

import org.springframework.stereotype.Component;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.client.UserRole;
import ru.itmo.ticketing.booking.common.ServiceUnavailableException;
import ru.itmo.ticketing.booking.common.UnauthorizedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Пользователи для тестов вместо user-service: те же ошибки, что у настоящего клиента.
 */
@Component
public class TestUsers {

    private final Map<Long, UserInfo> users = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean down;

    public long add(UserRole role) {
        long id = ids.incrementAndGet();
        users.put(id, new UserInfo(id, "user" + id + "@test", "Test " + role, role));
        return id;
    }

    public UserInfo get(long id) {
        return users.get(id);
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
