package ru.itmo.ticketing.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.ticketing.user.dto.UserResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private UserRepository users;

    @Test
    void registerAndReadSelf() {
        UserResponse created = client.post().uri("/api/users")
                .bodyValue(Map.of("email", "Ivan@test.ru", "fullName", "Ivan"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/users/\\d+")
                .expectBody(UserResponse.class)
                .returnResult().getResponseBody();

        assertThat(created.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(created.email()).isEqualTo("ivan@test.ru");
        assertThat(created.createdAt()).isNotNull();

        client.get().uri("/api/users/me").header("X-User-Id", String.valueOf(created.id()))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo("ivan@test.ru");
    }

    @Test
    void duplicateEmailIsConflict() {
        users.save(new User("dup@test.ru", "Dup", UserRole.CUSTOMER)).block();

        client.post().uri("/api/users")
                .bodyValue(Map.of("email", "DUP@test.ru", "fullName", "Dup"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void invalidBodyIsBadRequest() {
        client.post().uri("/api/users")
                .bodyValue(Map.of("email", "not-an-email", "fullName", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.fieldErrors.length()").isEqualTo(2);
    }

    @Test
    void missingOrUnknownUserIsUnauthorized() {
        client.get().uri("/api/users").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/users").header("X-User-Id", "999999").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/users").header("X-User-Id", "abc").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void customerCannotListUsers() {
        User customer = users.save(new User("c@test.ru", "C", UserRole.CUSTOMER)).block();

        client.get().uri("/api/users").header("X-User-Id", String.valueOf(customer.getId()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminChangesRoleAndListsUsers() {
        User admin = users.save(new User("admin@test.ru", "Admin", UserRole.ADMIN)).block();
        User target = users.save(new User("target@test.ru", "Target", UserRole.CUSTOMER)).block();

        client.patch().uri("/api/users/" + target.getId() + "/role")
                .header("X-User-Id", String.valueOf(admin.getId()))
                .bodyValue(Map.of("role", "ORGANIZER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("ORGANIZER");

        assertThat(users.findById(target.getId()).block().getRole()).isEqualTo(UserRole.ORGANIZER);

        client.get().uri("/api/users?size=500").header("X-User-Id", String.valueOf(admin.getId()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Total-Count")
                .expectBody()
                .jsonPath("$.size").isEqualTo(50)
                .jsonPath("$.content[0].id").isNumber();

        client.get().uri("/api/users?page=-1").header("X-User-Id", String.valueOf(admin.getId()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void adminDeletesUser() {
        User admin = users.save(new User("admin2@test.ru", "Admin", UserRole.ADMIN)).block();
        User target = users.save(new User("gone@test.ru", "Gone", UserRole.CUSTOMER)).block();

        client.delete().uri("/api/users/" + target.getId())
                .header("X-User-Id", String.valueOf(admin.getId()))
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/users/" + target.getId())
                .header("X-User-Id", String.valueOf(admin.getId()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void openApiIsServed() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/users/me'].get").exists()
                .jsonPath("$.components.securitySchemes['X-User-Id'].in").isEqualTo("header");
    }
}
