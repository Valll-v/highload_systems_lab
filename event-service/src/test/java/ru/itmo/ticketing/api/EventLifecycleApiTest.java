package ru.itmo.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.booking.CancelledBookings;
import ru.itmo.ticketing.common.ServiceUnavailableException;
import ru.itmo.ticketing.support.ApiClient;
import ru.itmo.ticketing.support.IntegrationTestBase;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static ru.itmo.ticketing.support.ApiClient.expect;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventLifecycleApiTest extends IntegrationTestBase {

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
        api = new ApiClient(mvc, json, users);
        admin = api.user("admin@flow.test", "ADMIN");
        organizer = api.user("org@flow.test", "ORGANIZER");
        customer = api.user("customer@flow.test", "CUSTOMER");
        category = api.category(admin, "Concerts");
        long venue = api.venue(admin, "Ice Palace", "Saint Petersburg");
        hall = api.hall(admin, venue, 5, 10);
    }

    @AfterEach
    void bookingServiceIsBack() {
        doAnswer(call -> new CancelledBookings(call.getArgument(0), 0)).when(bookingClient).cancelByEvent(anyLong(), anyLong());
    }

    @Test
    void publishedEventIsInCatalogue() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "Rock night");

        var catalogue = expect(api.get(null, "/api/events?city=saint petersburg"), 200);
        assertThat(catalogue.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(event);
        var unfiltered = expect(api.get(null, "/api/events"), 200);
        assertThat(unfiltered.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(event);
        assertThat(api.seats(hall)).hasSize(50);
    }

    @Test
    void eventLifecycle() throws Exception {
        var created = expect(api.post(organizer, "/api/events", api.eventBody(category, hall, "Draft")), 201);
        long id = created.id();
        assertThat(created.location()).endsWith("/api/events/" + id);
        assertThat(created.body().get("status").asText()).isEqualTo("DRAFT");

        assertThat(api.get(customer, "/api/events/" + id).status()).isEqualTo(404);
        assertThat(api.get(organizer, "/api/events/" + id).status()).isEqualTo(200);
        assertThat(api.post(admin, "/api/events/" + id + "/publish", null).status()).isEqualTo(409);

        expect(api.post(organizer, "/api/events/" + id + "/submit", null), 200);
        var queue = expect(api.get(admin, "/api/events/moderation"), 200);
        assertThat(queue.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(id);
        var mine = expect(api.get(organizer, "/api/events/my"), 200);
        assertThat(mine.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(id);

        expect(api.post(admin, "/api/events/" + id + "/reject", null), 200);
        expect(api.put(organizer, "/api/events/" + id, api.eventBody(category, hall, "Draft v2")), 200);
        expect(api.post(organizer, "/api/events/" + id + "/submit", null), 200);
        expect(api.post(admin, "/api/events/" + id + "/publish", null), 200);

        assertThat(api.put(organizer, "/api/events/" + id, api.eventBody(category, hall, "Draft v3")).status()).isEqualTo(409);
        expect(api.post(admin, "/api/events/" + id + "/cancel", null), 200);
        assertThat(api.post(admin, "/api/events/" + id + "/cancel", null).status()).isEqualTo(409);
    }

    @Test
    void cancellingEventTellsBookingService() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "To be cancelled");

        var cancelled = expect(api.post(organizer, "/api/events/" + event + "/cancel", null), 200);
        assertThat(cancelled.body().get("status").asText()).isEqualTo("CANCELLED");
        verify(bookingClient).cancelByEvent(event, organizer);
    }

    @Test
    void eventIsCancelledEvenWhenBookingServiceIsDown() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "Cancelled offline");
        doThrow(new ServiceUnavailableException("booking-service is unavailable"))
                .when(bookingClient).cancelByEvent(anyLong(), anyLong());

        expect(api.post(admin, "/api/events/" + event + "/cancel", null), 200);
        assertThat(expect(api.get(organizer, "/api/events/" + event), 200).body().get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void publishRejectsHallTimeConflict() throws Exception {
        long first = api.publishedEvent(organizer, admin, category, hall, "First");
        var firstBody = expect(api.get(null, "/api/events/" + first), 200).body();
        Map<String, Object> overlapping = new HashMap<>(api.eventBody(category, hall, "Overlapping"));
        overlapping.put("startsAt", firstBody.get("startsAt").asText());
        overlapping.put("endsAt", firstBody.get("endsAt").asText());
        long second = expect(api.post(organizer, "/api/events", overlapping), 201).id();
        expect(api.post(organizer, "/api/events/" + second + "/submit", null), 200);
        assertThat(api.post(admin, "/api/events/" + second + "/publish", null).status()).isEqualTo(409);
        expect(api.post(admin, "/api/events/" + first + "/cancel", null), 200);
        expect(api.post(admin, "/api/events/" + second + "/publish", null), 200);
    }
}
