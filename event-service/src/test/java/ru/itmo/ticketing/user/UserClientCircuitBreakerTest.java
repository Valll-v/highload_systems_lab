package ru.itmo.ticketing.user;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.itmo.ticketing.support.PostgresContainer;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Настоящий Feign-клиент: user-service играет WireMock, адрес подсунут через simple discovery
 * вместо Eureka. Проверяется сам вызов, ignore-exceptions и открытие выключателя.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserClientCircuitBreakerTest {

    static final WireMockServer USER_SERVICE = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        USER_SERVICE.start();
    }

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresContainer.INSTANCE;

    @DynamicPropertySource
    static void userServiceAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri", USER_SERVICE::baseUrl);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CircuitBreakerRegistry breakers;

    @BeforeEach
    void cleanSlate() {
        USER_SERVICE.resetAll();
        breaker().ifPresent(CircuitBreaker::reset);
        USER_SERVICE.stubFor(WireMock.get(urlPathEqualTo("/api/users/me")).withHeader("X-User-Id", equalTo("1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"email\":\"org@test\",\"fullName\":\"Org\",\"role\":\"ORGANIZER\"}")));
        USER_SERVICE.stubFor(WireMock.get(urlPathEqualTo("/api/users/me")).withHeader("X-User-Id", equalTo("2"))
                .willReturn(aResponse().withStatus(401)));
        USER_SERVICE.stubFor(WireMock.get(urlPathEqualTo("/api/users/me")).withHeader("X-User-Id", equalTo("3"))
                .willReturn(aResponse().withStatus(500)));
    }

    @AfterAll
    static void stopUserService() {
        USER_SERVICE.stop();
    }

    @Test
    void roleIsFetchedOverHttp() throws Exception {
        mvc.perform(get("/api/events/my").header("X-User-Id", 1))
                .andExpect(status().isOk());
        mvc.perform(get("/api/events/moderation").header("X-User-Id", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ORGANIZER")));

        USER_SERVICE.verify(2, getRequestedFor(urlPathEqualTo("/api/users/me")).withHeader("X-User-Id", equalTo("1")));
        assertThat(breaker().orElseThrow().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void unknownUserIsNotCountedAsFailure() throws Exception {
        for (int i = 0; i < 6; i++) {
            mvc.perform(get("/api/events/my").header("X-User-Id", 2)).andExpect(status().isUnauthorized());
        }
        CircuitBreaker breaker = breaker().orElseThrow();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void breakerOpensAfterServerErrorsAndStopsCalling() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/events/my").header("X-User-Id", 3))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("user-service is unavailable"));
        }
        CircuitBreaker breaker = breaker().orElseThrow();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        mvc.perform(get("/api/events/my").header("X-User-Id", 1)).andExpect(status().isServiceUnavailable());
        USER_SERVICE.verify(0, getRequestedFor(urlPathEqualTo("/api/users/me")).withHeader("X-User-Id", equalTo("1")));
        assertThat(breaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);

        mvc.perform(get("/api/events")).andExpect(status().isOk());

        breaker.transitionToClosedState();
        mvc.perform(get("/api/events/my").header("X-User-Id", 1)).andExpect(status().isOk());
    }

    private Optional<CircuitBreaker> breaker() {
        return breakers.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().contains("UserClient"))
                .findFirst();
    }
}
