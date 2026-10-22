package ru.itmo.ticketing.booking.common;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.itmo.ticketing.booking.client.UserClient;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.client.UserRole;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-User-Id";

    private final UserClient users;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserInfo.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
                                        ServerWebExchange exchange) {
        UserRole[] allowed = parameter.getParameterAnnotation(CurrentUser.class).value();
        return resolve(exchange.getRequest().getHeaders().getFirst(HEADER), allowed).cast(Object.class);
    }

    public Mono<UserInfo> resolve(String raw, UserRole[] allowed) {
        if (raw == null || raw.isBlank()) {
            return Mono.error(new UnauthorizedException("Missing " + HEADER + " header"));
        }
        long id;
        try {
            id = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return Mono.error(new UnauthorizedException(HEADER + " must be a number"));
        }
        return Blocking.call(() -> users.me(id))
                .flatMap(user -> {
                    if (allowed.length == 0 || Arrays.asList(allowed).contains(user.role())) {
                        return Mono.just(user);
                    }
                    return Mono.error(new ForbiddenException(
                            "Requires role " + Arrays.toString(allowed) + ", current role is " + user.role()));
                });
    }
}
