package ru.itmo.ticketing.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @AfterEach
    void userServiceIsBack() {
        users.down(false);
    }

    @Test
    void missingOrUnknownUserIsUnauthorized() throws Exception {
        mvc.perform(get("/api/events/my")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/events/my").header("X-User-Id", 999999)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/events/my").header("X-User-Id", "abc")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIsForbidden() throws Exception {
        long customer = users.add(UserRole.CUSTOMER, "customer@cu.test");
        mvc.perform(get("/api/events/my").header("X-User-Id", customer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ORGANIZER")));
    }

    @Test
    void protectedEndpointsFailFastWhenUserServiceIsDown() throws Exception {
        long organizer = users.add(UserRole.ORGANIZER, "org@cu.test");
        users.down(true);

        mvc.perform(get("/api/events/my").header("X-User-Id", organizer))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("user-service is unavailable"));
    }

    @Test
    void publicEndpointsTreatClientAsAnonymousWhenUserServiceIsDown() throws Exception {
        users.down(true);

        mvc.perform(get("/api/events")).andExpect(status().isOk());
        mvc.perform(get("/api/events/424242").header("X-User-Id", 1)).andExpect(status().isNotFound());
    }
}
