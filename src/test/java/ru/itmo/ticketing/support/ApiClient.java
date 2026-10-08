package ru.itmo.ticketing.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiClient {

    public record Response(int status, JsonNode body) {
        public long id() {
            return body.get("id").asLong();
        }
    }

    private static final AtomicInteger SLOT = new AtomicInteger();

    private final MockMvc mvc;
    private final ObjectMapper json;

    public ApiClient(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    public Response post(Long userId, String path, Object body) throws Exception {
        return exchange(MockMvcRequestBuilders.post(path), userId, body);
    }

    public Response put(Long userId, String path, Object body) throws Exception {
        return exchange(MockMvcRequestBuilders.put(path), userId, body);
    }

    public Response patch(Long userId, String path, Object body) throws Exception {
        return exchange(MockMvcRequestBuilders.patch(path), userId, body);
    }

    public Response get(Long userId, String path) throws Exception {
        return exchange(MockMvcRequestBuilders.get(path), userId, null);
    }

    public Response delete(Long userId, String path) throws Exception {
        return exchange(MockMvcRequestBuilders.delete(path), userId, null);
    }

    private Response exchange(MockHttpServletRequestBuilder builder, Long userId, Object body) throws Exception {
        if (userId != null) {
            builder.header("X-User-Id", userId);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(builder).andReturn();
        String content = result.getResponse().getContentAsString();
        JsonNode node = content.isBlank() ? json.nullNode() : json.readTree(content);
        return new Response(result.getResponse().getStatus(), node);
    }

    public long user(String email, String role) throws Exception {
        return expect(post(null, "/api/users", Map.of("email", email, "fullName", "Test " + role, "role", role)), 201).id();
    }

    public long category(long admin, String name) throws Exception {
        return expect(post(admin, "/api/categories", Map.of("name", name)), 201).id();
    }

    public long venue(long admin, String name, String city) throws Exception {
        return expect(post(admin, "/api/venues", Map.of("name", name, "city", city, "address", "Kronverksky 49")), 201).id();
    }

    public long hall(long admin, long venueId, int rows, int seatsPerRow) throws Exception {
        return expect(post(admin, "/api/venues/" + venueId + "/halls",
                Map.of("name", "Hall " + rows + "x" + seatsPerRow, "rows", rows, "seatsPerRow", seatsPerRow)), 201).id();
    }

    public List<Long> seats(long hallId) throws Exception {
        Response r = expect(get(null, "/api/halls/" + hallId + "/seats"), 200);
        List<Long> ids = new ArrayList<>();
        r.body().forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    public Map<String, Object> eventBody(long categoryId, long hallId, String title) {
        OffsetDateTime start = OffsetDateTime.now().plusDays(7).plusHours(3L * SLOT.incrementAndGet());
        return Map.of(
                "title", title,
                "description", "desc",
                "categoryId", categoryId,
                "hallId", hallId,
                "startsAt", start.toString(),
                "endsAt", start.plusHours(2).toString(),
                "price", new BigDecimal("1500.00"));
    }

    public long publishedEvent(long organizer, long admin, long categoryId, long hallId, String title) throws Exception {
        long id = expect(post(organizer, "/api/events", eventBody(categoryId, hallId, title)), 201).id();
        expect(post(organizer, "/api/events/" + id + "/submit", null), 200);
        expect(post(admin, "/api/events/" + id + "/publish", null), 200);
        return id;
    }

    public static Response expect(Response r, int status) {
        if (r.status() != status) {
            throw new AssertionError("expected " + status + " but got " + r.status() + ": " + r.body());
        }
        return r;
    }
}
