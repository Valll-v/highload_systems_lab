package ru.itmo.ticketing.user;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import ru.itmo.ticketing.common.ServiceUnavailableException;
import ru.itmo.ticketing.common.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserClientTest {

    private final ErrorDecoder decoder = new UserClientConfig().userErrorDecoder();
    private final UserClientFallback fallback = new UserClientFallback();

    @Test
    void unknownUserBecomesUnauthorized() {
        Exception e = decoder.decode("UserClient#me(long)", response(401));
        assertThat(e).isInstanceOf(UnauthorizedException.class).hasMessageContaining("42");
        assertThat(decoder.decode("UserClient#me(long)", response(404))).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void otherErrorsStayFeignErrors() {
        assertThat(decoder.decode("UserClient#me(long)", response(500))).isInstanceOf(feign.FeignException.class);
    }

    @Test
    void fallbackKeepsUnauthorizedAndHidesEverythingElse() {
        assertThatThrownBy(() -> fallback.create(new UnauthorizedException("Unknown user 1")).me(1))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> fallback.create(new RuntimeException("connection refused")).me(1))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private static Response response(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://user-service/api/users/me",
                Map.of("X-User-Id", List.of("42")), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).request(request).build();
    }
}
