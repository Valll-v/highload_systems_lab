package ru.itmo.ticketing.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", configuration = UserClientConfig.class, fallbackFactory = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/me")
    UserInfo me(@RequestHeader("X-User-Id") long userId);
}
