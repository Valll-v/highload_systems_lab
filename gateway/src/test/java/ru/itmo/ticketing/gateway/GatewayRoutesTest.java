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
                .jsonPath("$.urls[0].name").isEqualTo("event-service")
                .jsonPath("$.urls[0].url").isEqualTo("/event-service/v3/api-docs")
                .jsonPath("$.urls[1].name").isEqualTo("user-service")
                .jsonPath("$.urls[1].url").isEqualTo("/user-service/v3/api-docs");

        client.get().uri("/swagger-ui.html").exchange()
                .expectStatus().is3xxRedirection();
    }
}
