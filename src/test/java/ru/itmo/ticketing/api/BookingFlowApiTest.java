package ru.itmo.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.support.ApiClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.itmo.ticketing.support.ApiClient.expect;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingFlowApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiClient api;
    private long admin;
    private long organizer;
    private long customer;
    private long category;
    private long hall;
    private List<Long> seats;

    @BeforeAll
    void setUp() throws Exception {
        api = new ApiClient(mvc, json);
        admin = api.user("admin@flow.test", "ADMIN");
        organizer = api.user("org@flow.test", "ORGANIZER");
        customer = api.user("customer@flow.test", "CUSTOMER");
        category = api.category(admin, "Concerts");
        long venue = api.venue(admin, "Ice Palace", "Saint Petersburg");
        hall = api.hall(admin, venue, 5, 10);
        seats = api.seats(hall);
    }

    @Test
    void fullScenario() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "Rock night");

        var catalogue = expect(api.get(null, "/api/events?city=saint petersburg"), 200);
        assertThat(catalogue.body().get("content").findValues("id")).extracting(n -> n.asLong()).contains(event);
        var seatMap = expect(api.get(null, "/api/events/" + event + "/seats"), 200);
        assertThat(seatMap.body().findValues("available")).allMatch(n -> n.asBoolean());

        List<Long> chosen = seats.subList(0, 2);
        var booking = expect(api.post(customer, "/api/bookings", Map.of("eventId", event, "seatIds", chosen)), 201);
        long bookingId = booking.id();
        assertThat(booking.body().get("status").asText()).isEqualTo("ACTIVE");
        assertThat(booking.body().get("totalPrice").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(booking.body().get("tickets")).hasSize(2);

        seatMap = expect(api.get(null, "/api/events/" + event + "/seats"), 200);
        long taken = seatMap.body().findValues("available").stream().filter(n -> !n.asBoolean()).count();
        assertThat(taken).isEqualTo(2);

        long other = api.user("other@flow.test", "CUSTOMER");
        var conflict = api.post(other, "/api/bookings", Map.of("eventId", event, "seatIds", List.of(chosen.get(0))));
        assertThat(conflict.status()).isEqualTo(409);

        var sales = expect(api.get(organizer, "/api/events/" + event + "/sales"), 200);
        assertThat(sales.body().get("ticketsSold").asLong()).isEqualTo(2);
        assertThat(sales.body().get("seatsAvailable").asLong()).isEqualTo(48);
        assertThat(sales.body().get("revenue").decimalValue()).isEqualByComparingTo("3000.00");

        var tickets = expect(api.get(customer, "/api/tickets"), 200);
        assertThat(tickets.body()).hasSize(2);
        assertThat(tickets.body().get(0).get("code").asText()).startsWith("TKT-");

        var cancelled = expect(api.post(customer, "/api/bookings/" + bookingId + "/cancel", null), 200);
        assertThat(cancelled.body().get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.body().get("tickets").findValues("status")).allMatch(n -> n.asText().equals("CANCELLED"));
        seatMap = expect(api.get(null, "/api/events/" + event + "/seats"), 200);
        assertThat(seatMap.body().findValues("available")).allMatch(n -> n.asBoolean());
        assertThat(api.post(customer, "/api/bookings/" + bookingId + "/cancel", null).status()).isEqualTo(409);

        expect(api.post(other, "/api/bookings", Map.of("eventId", event, "seatIds", List.of(chosen.get(0)))), 201);
    }

    @Test
    void failedBookingIsRolledBack() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "Atomicity check");
        long before = jdbc.queryForObject("select count(*) from bookings", Long.class);

        long otherHall = api.hall(admin, api.venue(admin, "Other", "Moscow"), 1, 1);
        Long foreignSeat = api.seats(otherHall).get(0);
        var r = api.post(customer, "/api/bookings", Map.of("eventId", event, "seatIds", List.of(seats.get(10), foreignSeat)));
        assertThat(r.status()).isEqualTo(409);

        assertThat(jdbc.queryForObject("select count(*) from bookings", Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from seat_reservations where event_id = ?", Long.class, event)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from tickets where event_id = ?", Long.class, event)).isZero();
    }

    @Test
    void cancellingEventCancelsBookings() throws Exception {
        long event = api.publishedEvent(organizer, admin, category, hall, "To be cancelled");
        long booking = expect(api.post(customer, "/api/bookings", Map.of("eventId", event, "seatIds", List.of(seats.get(20)))), 201).id();

        expect(api.post(organizer, "/api/events/" + event + "/cancel", null), 200);

        var b = expect(api.get(customer, "/api/bookings/" + booking), 200);
        assertThat(b.body().get("status").asText()).isEqualTo("CANCELLED");
        assertThat(api.post(customer, "/api/bookings", Map.of("eventId", event, "seatIds", List.of(seats.get(21)))).status()).isEqualTo(409);
    }

    @Test
    void eventLifecycle() throws Exception {
        var created = expect(api.post(organizer, "/api/events", api.eventBody(category, hall, "Draft")), 201);
        long id = created.id();
        assertThat(created.body().get("status").asText()).isEqualTo("DRAFT");

        assertThat(api.get(customer, "/api/events/" + id).status()).isEqualTo(404);
        assertThat(api.get(organizer, "/api/events/" + id).status()).isEqualTo(200);
        assertThat(api.post(admin, "/api/events/" + id + "/publish", null).status()).isEqualTo(409);

        expect(api.post(organizer, "/api/events/" + id + "/submit", null), 200);
        var queue = expect(api.get(admin, "/api/events/moderation"), 200);
        assertThat(queue.body().findValues("id")).extracting(n -> n.asLong()).contains(id);

        expect(api.post(admin, "/api/events/" + id + "/reject", null), 200);
        expect(api.put(organizer, "/api/events/" + id, api.eventBody(category, hall, "Draft v2")), 200);
        expect(api.post(organizer, "/api/events/" + id + "/submit", null), 200);
        expect(api.post(admin, "/api/events/" + id + "/publish", null), 200);

        assertThat(api.put(organizer, "/api/events/" + id, api.eventBody(category, hall, "Draft v3")).status()).isEqualTo(409);

        var draft = expect(api.post(organizer, "/api/events", api.eventBody(category, hall, "Unpublished")), 201);
        assertThat(api.post(customer, "/api/bookings", Map.of("eventId", draft.id(), "seatIds", List.of(seats.get(30)))).status()).isEqualTo(409);
        expect(api.post(admin, "/api/events/" + id + "/cancel", null), 200);
    }

    @Test
    void publishRejectsHallTimeConflict() throws Exception {
        long first = api.publishedEvent(organizer, admin, category, hall, "First");
        var firstBody = expect(api.get(null, "/api/events/" + first), 200).body();
        Map<String, Object> overlapping = new java.util.HashMap<>(api.eventBody(category, hall, "Overlapping"));
        overlapping.put("startsAt", firstBody.get("startsAt").asText());
        overlapping.put("endsAt", firstBody.get("endsAt").asText());
        long second = expect(api.post(organizer, "/api/events", overlapping), 201).id();
        expect(api.post(organizer, "/api/events/" + second + "/submit", null), 200);
        assertThat(api.post(admin, "/api/events/" + second + "/publish", null).status()).isEqualTo(409);
        expect(api.post(admin, "/api/events/" + first + "/cancel", null), 200);
        expect(api.post(admin, "/api/events/" + second + "/publish", null), 200);
    }
}
