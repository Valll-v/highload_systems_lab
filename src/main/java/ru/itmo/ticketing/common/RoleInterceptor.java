package ru.itmo.ticketing.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;

import java.util.Arrays;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final CurrentUserResolver resolver;

    public RoleInterceptor(CurrentUserResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole required = method.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required == null) {
            return true;
        }
        User user = resolver.resolve(request);
        UserRole[] allowed = required.value();
        if (Arrays.stream(allowed).noneMatch(r -> r == user.getRole())) {
            throw new ForbiddenException("Requires role " + Arrays.toString(allowed) + ", current role is " + user.getRole());
        }
        return true;
    }
}
