package ru.itmo.ticketing.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.itmo.ticketing.user.UserClient;
import ru.itmo.ticketing.user.UserInfo;

import java.util.Optional;

/**
 * Пользователь из заголовка X-User-Id. Роль спрашивается у user-service один раз на запрос,
 * результат лежит в атрибуте запроса.
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-User-Id";
    static final String REQUEST_ATTRIBUTE = CurrentUserResolver.class.getName() + ".user";

    private final UserClient users;

    /**
     * Клиент ленивый: Feign-бин сам требует веб-конфигурацию, а она требует этот резолвер,
     * без @Lazy контекст не стартует из-за цикла.
     */
    public CurrentUserResolver(@Lazy UserClient users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserInfo.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return resolve(request);
    }

    public UserInfo resolve(HttpServletRequest request) {
        Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached instanceof UserInfo user) {
            return user;
        }
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Missing " + HEADER + " header");
        }
        long id;
        try {
            id = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException(HEADER + " must be a number");
        }
        UserInfo user = users.me(id);
        request.setAttribute(REQUEST_ATTRIBUTE, user);
        return user;
    }

    /**
     * Для публичных эндпоинтов: нет заголовка, неизвестный пользователь или лежащий
     * user-service означают анонимного клиента.
     */
    public Optional<UserInfo> optional(HttpServletRequest request) {
        if (request.getHeader(HEADER) == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(resolve(request));
        } catch (UnauthorizedException | ServiceUnavailableException e) {
            return Optional.empty();
        }
    }
}
