package ru.itmo.ticketing.booking.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Feign декодирует ответы MVC-конвертерами, а в WebFlux-приложении Spring Boot их не создаёт.
 * Без этого бина любой успешный ответ другого сервиса падает в DecodeException.
 */
@Configuration
public class FeignConfig {

    @Bean
    public HttpMessageConverters feignMessageConverters(ObjectMapper objectMapper) {
        return new HttpMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
