package ru.itmo.ticketing.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.IntegrationTestBase;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Test
    void registerAndReadSelf() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ivan@test.ru\",\"fullName\":\"Ivan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        Long id = users.findByEmailIgnoreCase("ivan@test.ru").orElseThrow().getId();

        mvc.perform(get("/api/users/me").header("X-User-Id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ivan@test.ru"));
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        users.save(new User("dup@test.ru", "Dup", UserRole.CUSTOMER));

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"DUP@test.ru\",\"fullName\":\"Dup\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidBodyIsBadRequest() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    void missingHeaderIsUnauthorized() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("X-User-Id", 999999)).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotListUsers() throws Exception {
        User customer = users.save(new User("c@test.ru", "C", UserRole.CUSTOMER));

        mvc.perform(get("/api/users").header("X-User-Id", customer.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminChangesRole() throws Exception {
        User admin = users.save(new User("admin@test.ru", "Admin", UserRole.ADMIN));
        User target = users.save(new User("target@test.ru", "Target", UserRole.CUSTOMER));

        mvc.perform(patch("/api/users/" + target.getId() + "/role")
                        .header("X-User-Id", admin.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));

        mvc.perform(get("/api/users").header("X-User-Id", admin.getId()))
                .andExpect(status().isOk());
    }
}
