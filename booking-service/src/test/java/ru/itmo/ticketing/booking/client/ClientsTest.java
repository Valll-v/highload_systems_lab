package ru.itmo.ticketing.booking.client;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import ru.itmo.ticketing.booking.common.NotFoundException;
import ru.itmo.ticketing.booking.common.ServiceUnavailableException;
import ru.itmo.ticketing.booking.common.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientsTest {

    @Test
    void userErrorsAreTranslated() {
        var decoder = new UserClientConfig().userErrorDecoder();
        assertThat(decoder.decode("UserClient#me(long)", response(401, "http://user-service/api/users/me")))
                .isInstanceOf(UnauthorizedException.class).hasMessageContaining("42");
        assertThat(decoder.decode("UserClient#me(long)", response(500, "http://user-service/api/users/me")))
                .isInstanceOf(feign.FeignException.class);

        var fallback = new UserClientFallback();
        assertThatThrownBy(() -> fallback.create(new UnauthorizedException("x")).me(1)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> fallback.create(new RuntimeException("down")).me(1)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void eventErrorsAreTranslated() {
        var decoder = new EventClientConfig().eventErrorDecoder();
        assertThat(decoder.decode("EventClient#event(long)", response(404, "http://event-service/api/events/7")))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("/api/events/7");
        assertThat(decoder.decode("EventClient#event(long)", response(500, "http://event-service/api/events/7")))
                .isInstanceOf(feign.FeignException.class);

        var fallback = new EventClientFallback();
        EventClient notFound = fallback.create(new NotFoundException("Event", 7));
        assertThatThrownBy(() -> notFound.event(7)).isInstanceOf(NotFoundException.class);
        EventClient down = fallback.create(new RuntimeException("down"));
        assertThatThrownBy(() -> down.eventAs(7, 1)).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> down.hall(1)).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> down.seats(1, 0, 50)).isInstanceOf(ServiceUnavailableException.class);
    }

    private static Response response(int status, String url) {
        Request request = Request.create(Request.HttpMethod.GET, url,
                Map.of("X-User-Id", List.of("42")), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).request(request).build();
    }
}
