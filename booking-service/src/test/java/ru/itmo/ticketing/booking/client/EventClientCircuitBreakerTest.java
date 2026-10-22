package ru.itmo.ticketing.booking.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Настоящий Feign-клиент к event-service, который играет WireMock. Схема мест собирается
 * из ответа event-service и пустой таблицы резервов, 404 не считается ошибкой,
 * пять 500 открывают выключатель.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class EventClientCircuitBreakerTest {

    static final WireMockServer EVENT_SERVICE = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        EVENT_SERVICE.start();
    }

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void eventServiceAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.event-service[0].uri", EVENT_SERVICE::baseUrl);
    }

    @Autowired
    private WebTestClient client;

    @Autowired
    private CircuitBreakerRegistry breakers;

    @BeforeEach
    void cleanSlate() {
        EVENT_SERVICE.resetAll();
        breaker().ifPresent(CircuitBreaker::reset);
        EVENT_SERVICE.stubFor(get(urlPathEqualTo("/api/events/5"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":5,\"title\":\"Show\",\"organizerId\":1,\"hallId\":9,"
                                + "\"startsAt\":\"2030-01-01T19:00:00+03:00\",\"endsAt\":\"2030-01-01T21:00:00+03:00\","
                                + "\"price\":100.00,\"status\":\"PUBLISHED\"}")));
        EVENT_SERVICE.stubFor(get(urlPathEqualTo("/api/halls/9/seats"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"content\":[{\"id\":1,\"row\":1,\"number\":1},{\"id\":2,\"row\":1,\"number\":2}],"
                                + "\"page\":0,\"size\":50,\"hasNext\":false}")));
        EVENT_SERVICE.stubFor(get(urlPathEqualTo("/api/events/6")).willReturn(aResponse().withStatus(404)));
        EVENT_SERVICE.stubFor(get(urlPathEqualTo("/api/events/7")).willReturn(aResponse().withStatus(500)));
    }

    @AfterAll
    static void stop() {
        EVENT_SERVICE.stop();
        POSTGRES.stop();
    }

    @Test
    void seatMapIsBuiltFromEventServiceAnswer() {
        client.get().uri("/api/events/5/seats").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].available").isEqualTo(true)
                .jsonPath("$.hasNext").isEqualTo(false);
        EVENT_SERVICE.verify(1, getRequestedFor(urlPathEqualTo("/api/halls/9/seats")).withQueryParam("size", equalTo("50")));
    }

    @Test
    void notFoundIsPassedThroughAndNotCounted() {
        for (int i = 0; i < 6; i++) {
            client.get().uri("/api/events/6/seats").exchange().expectStatus().isNotFound();
        }
        CircuitBreaker breaker = breaker().orElseThrow();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void breakerOpensAfterServerErrors() {
        for (int i = 0; i < 5; i++) {
            client.get().uri("/api/events/7/seats").exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody().jsonPath("$.message").isEqualTo("event-service is unavailable");
        }
        CircuitBreaker breaker = breaker().orElseThrow();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        client.get().uri("/api/events/5/seats").exchange().expectStatus().isEqualTo(503);
        EVENT_SERVICE.verify(0, getRequestedFor(urlPathEqualTo("/api/events/5")));

        breaker.transitionToClosedState();
        client.get().uri("/api/events/5/seats").exchange().expectStatus().isOk();
    }

    /** Выключатель на каждый метод клиента свой, нужен тот, что обслуживает event(long). */
    private Optional<CircuitBreaker> breaker() {
        return breakers.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().contains("EventClient"))
                .filter(b -> !b.getName().contains("eventAs") && !b.getName().contains("hall") && !b.getName().contains("seats"))
                .findFirst();
    }
}
