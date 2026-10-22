package ru.itmo.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.ApiClient;
import ru.itmo.ticketing.support.IntegrationTestBase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.itmo.ticketing.support.ApiClient.expect;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessControlApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient api;
    private long admin;
    private long organizer;
    private long customer;

    @BeforeAll
    void setUp() throws Exception {
        api = new ApiClient(mvc, json, users);
        admin = api.user("admin@acl.test", "ADMIN");
        organizer = api.user("org@acl.test", "ORGANIZER");
        customer = api.user("customer@acl.test", "CUSTOMER");
    }

    @Test
    void wrongRoleIsForbidden() throws Exception {
        assertThat(api.post(customer, "/api/categories", Map.of("name", "x")).status()).isEqualTo(403);
        assertThat(api.post(admin, "/api/events", Map.of()).status()).isEqualTo(403);
        assertThat(api.get(customer, "/api/events/moderation").status()).isEqualTo(403);
        assertThat(api.get(customer, "/api/events/my").status()).isEqualTo(403);
    }

    @Test
    void organizerCannotTouchForeignEvent() throws Exception {
        long category = api.category(admin, "ACL");
        long hall = api.hall(admin, api.venue(admin, "Club", "Perm"), 1, 3);
        long event = expect(api.post(organizer, "/api/events", api.eventBody(category, hall, "Mine")), 201).id();
        long intruder = api.user("intruder@acl.test", "ORGANIZER");
        assertThat(api.post(intruder, "/api/events/" + event + "/submit", null).status()).isEqualTo(403);
        assertThat(api.put(intruder, "/api/events/" + event, api.eventBody(category, hall, "Hijack")).status()).isEqualTo(403);
    }

    @Test
    void duplicateCategoryIsConflict() throws Exception {
        api.category(admin, "Theatre");
        assertThat(api.post(admin, "/api/categories", Map.of("name", "theatre")).status()).isEqualTo(409);
    }
}
