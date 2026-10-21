package ru.itmo.ticketing.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.ApiClient;
import ru.itmo.ticketing.support.IntegrationTestBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.itmo.ticketing.support.ApiClient.expect;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient api;
    private long admin;
    private long organizer;
    private long customer;
    private long category;
    private long hall;

    @BeforeAll
    void setUp() throws Exception {
        api = new ApiClient(mvc, json);
        admin = api.user("admin@tag.test", "ADMIN");
        organizer = api.user("org@tag.test", "ORGANIZER");
        customer = api.user("customer@tag.test", "CUSTOMER");
        category = api.category(admin, "Tagged");
        hall = api.hall(admin, api.venue(admin, "Tag Hall", "Kazan"), 2, 2);
    }

    @Test
    void adminManagesTags() throws Exception {
        var created = expect(api.post(admin, "/api/tags", Map.of("name", "  Jazz ")), 201);
        long id = created.id();
        assertThat(created.location()).endsWith("/api/tags/" + id);
        assertThat(created.body().get("name").asText()).isEqualTo("Jazz");

        assertThat(api.post(admin, "/api/tags", Map.of("name", "jazz")).status()).isEqualTo(409);
        assertThat(api.post(customer, "/api/tags", Map.of("name", "Blues")).status()).isEqualTo(403);
        assertThat(api.post(admin, "/api/tags", Map.of("name", "")).status()).isEqualTo(400);

        var renamed = expect(api.put(admin, "/api/tags/" + id, Map.of("name", "Jazz night")), 200);
        assertThat(renamed.body().get("name").asText()).isEqualTo("Jazz night");

        var list = expect(api.get(null, "/api/tags?size=100"), 200);
        assertThat(list.body().get("size").asInt()).isEqualTo(50);
        assertThat(list.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(id);

        expect(api.delete(admin, "/api/tags/" + id), 204);
        assertThat(api.get(null, "/api/tags/" + id).status()).isEqualTo(404);
    }

    @Test
    void eventCarriesTags() throws Exception {
        long rock = expect(api.post(admin, "/api/tags", Map.of("name", "Rock")), 201).id();
        long live = expect(api.post(admin, "/api/tags", Map.of("name", "Live")), 201).id();

        Map<String, Object> body = new HashMap<>(api.eventBody(category, hall, "Tagged show"));
        body.put("tagIds", List.of(rock, live));
        var created = expect(api.post(organizer, "/api/events", body), 201);
        long event = created.id();
        assertThat(created.body().get("tags").findValues("name")).extracting(n -> n.asText()).containsExactly("Live", "Rock");

        body.put("tagIds", List.of(rock));
        var updated = expect(api.put(organizer, "/api/events/" + event, body), 200);
        assertThat(updated.body().get("tags").findValues("id")).extracting(n -> n.asLong()).containsExactly(rock);

        long other = expect(api.post(organizer, "/api/events", body), 201).id();
        assertThat(api.get(organizer, "/api/events/" + other).body().get("tags")).hasSize(1);

        body.put("tagIds", List.of(rock, 424242L));
        assertThat(api.put(organizer, "/api/events/" + event, body).status()).isEqualTo(404);

        expect(api.delete(admin, "/api/tags/" + rock), 204);
        assertThat(api.get(organizer, "/api/events/" + event).body().get("tags")).isEmpty();
        assertThat(api.get(organizer, "/api/events/" + other).body().get("tags")).isEmpty();
    }
}
