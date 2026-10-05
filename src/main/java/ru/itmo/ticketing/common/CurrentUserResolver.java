package ru.itmo.ticketing.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRepository;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-User-Id";
    static final String REQUEST_ATTRIBUTE = CurrentUserResolver.class.getName() + ".user";

    private final UserRepository users;

    public CurrentUserResolver(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return resolve(request);
    }

    public User resolve(HttpServletRequest request) {
        Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached instanceof User user) {
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
        User user = users.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Unknown user " + id));
        request.setAttribute(REQUEST_ATTRIBUTE, user);
        return user;
    }
}
