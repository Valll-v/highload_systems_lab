package ru.itmo.ticketing.user;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import ru.itmo.ticketing.common.UnauthorizedException;

import java.util.Collection;

/**
 * Не помечен как @Configuration нарочно: подключается только к UserClient.
 */
public class UserClientConfig {

    @Bean
    public ErrorDecoder userErrorDecoder() {
        ErrorDecoder other = new ErrorDecoder.Default();
        return (methodKey, response) -> switch (response.status()) {
            case 401, 404 -> new UnauthorizedException("Unknown user " + userId(response));
            default -> other.decode(methodKey, response);
        };
    }

    private static String userId(Response response) {
        Collection<String> values = response.request().headers().get("X-User-Id");
        return values == null || values.isEmpty() ? "?" : values.iterator().next();
    }
}
