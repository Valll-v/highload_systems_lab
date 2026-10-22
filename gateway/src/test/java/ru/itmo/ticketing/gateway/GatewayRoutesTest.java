package ru.itmo.ticketing.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private WebTestClient client;

    @Test
    void apiIsRoutedToServices() {
        client.get().uri("/api/events").exchange()
                .expectStatus().isEqualTo(503);
        client.get().uri("/api/users/me").exchange()
                .expectStatus().isEqualTo(503);
        client.get().uri("/api/events/1/seats").exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void unknownPathIsNotFound() {
        client.get().uri("/something").exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void swaggerListsServices() {
        client.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls.length()").isEqualTo(3)
                .jsonPath("$.urls[?(@.name == 'event-service' && @.url == '/event-service/v3/api-docs')]").exists()
                .jsonPath("$.urls[?(@.name == 'user-service' && @.url == '/user-service/v3/api-docs')]").exists()
                .jsonPath("$.urls[?(@.name == 'booking-service' && @.url == '/booking-service/v3/api-docs')]").exists();

        client.get().uri("/swagger-ui.html").exchange()
                .expectStatus().is3xxRedirection();
    }
}
