package ru.itmo.ticketing.booking.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import ru.itmo.ticketing.booking.common.NotFoundException;

import java.net.URI;

/**
 * Не помечен как @Configuration нарочно: подключается только к EventClient.
 */
public class EventClientConfig {

    @Bean
    public ErrorDecoder eventErrorDecoder() {
        ErrorDecoder other = new ErrorDecoder.Default();
        return (methodKey, response) -> response.status() == 404
                ? new NotFoundException("event-service has no " + URI.create(response.request().url()).getPath())
                : other.decode(methodKey, response);
    }
}
