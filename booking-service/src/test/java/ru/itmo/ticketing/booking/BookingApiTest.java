package ru.itmo.ticketing.booking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.ticketing.booking.client.EventStatus;
import ru.itmo.ticketing.booking.client.UserRole;
import ru.itmo.ticketing.booking.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BookingApiTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long organizer;
    private long customer;
    private long hall;
    private List<Long> seats;

    @BeforeAll
    void setUp() {
        admin = users.add(UserRole.ADMIN);
        organizer = users.add(UserRole.ORGANIZER);
        customer = users.add(UserRole.CUSTOMER);
        hall = events.hall(5, 10);
        seats = events.seatIds(hall);
    }

    @AfterEach
    void servicesAreBack() {
        users.down(false);
        events.down(false);
    }

    private long published() {
        return events.event(organizer, hall, EventStatus.PUBLISHED, new BigDecimal("1500.00"));
    }

    private WebTestClient.ResponseSpec book(long user, long event, List<Long> seatIds) {
        return client.post().uri("/api/bookings").header("X-User-Id", String.valueOf(user))
                .bodyValue(Map.of("eventId", event, "seatIds", seatIds)).exchange();
    }

    private JsonNode body(WebTestClient.ResponseSpec response) {
        return response.expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    void fullScenario() {
        long event = published();

        JsonNode seatMap = body(client.get().uri("/api/events/" + event + "/seats?size=50").exchange().expectStatus().isOk());
        assertThat(seatMap.get("content")).hasSize(50);
        assertThat(seatMap.get("hasNext").asBoolean()).isFalse();
        assertThat(seatMap.findValues("available")).allMatch(JsonNode::asBoolean);

        List<Long> chosen = seats.subList(0, 2);
        JsonNode booking = body(book(customer, event, chosen).expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/bookings/\\d+"));
        long bookingId = booking.get("id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(booking.get("eventTitle").asText()).startsWith("Event ");
        assertThat(booking.get("totalPrice").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(booking.get("tickets")).hasSize(2);
        assertThat(booking.get("tickets").get(0).get("row").asInt()).isEqualTo(1);

        seatMap = body(client.get().uri("/api/events/" + event + "/seats?size=50").exchange().expectStatus().isOk());
        assertThat(seatMap.findValues("available").stream().filter(n -> !n.asBoolean()).count()).isEqualTo(2);

        long other = users.add(UserRole.CUSTOMER);
        book(other, event, List.of(chosen.get(0))).expectStatus().isEqualTo(409);

        JsonNode sales = body(client.get().uri("/api/events/" + event + "/sales").header("X-User-Id", String.valueOf(organizer))
                .exchange().expectStatus().isOk());
        assertThat(sales.get("capacity").asInt()).isEqualTo(50);
        assertThat(sales.get("ticketsSold").asLong()).isEqualTo(2);
        assertThat(sales.get("seatsAvailable").asLong()).isEqualTo(48);
        assertThat(sales.get("activeBookings").asLong()).isEqualTo(1);
        assertThat(sales.get("revenue").decimalValue()).isEqualByComparingTo("3000.00");

        JsonNode tickets = body(client.get().uri("/api/tickets").header("X-User-Id", String.valueOf(customer))
                .exchange().expectStatus().isOk().expectHeader().exists("X-Total-Count"));
        assertThat(tickets.get("content").findValues("bookingId")).extracting(JsonNode::asLong).contains(bookingId);
        assertThat(tickets.get("content").get(0).get("code").asText()).startsWith("TKT-");

        JsonNode mine = body(client.get().uri("/api/bookings").header("X-User-Id", String.valueOf(customer))
                .exchange().expectStatus().isOk());
        assertThat(mine.get("content").findValues("id")).extracting(JsonNode::asLong).contains(bookingId);

        JsonNode cancelled = body(client.post().uri("/api/bookings/" + bookingId + "/cancel")
                .header("X-User-Id", String.valueOf(customer)).exchange().expectStatus().isOk());
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("tickets").findValues("status")).allMatch(n -> n.asText().equals("CANCELLED"));
        client.post().uri("/api/bookings/" + bookingId + "/cancel").header("X-User-Id", String.valueOf(customer))
                .exchange().expectStatus().isEqualTo(409);

        seatMap = body(client.get().uri("/api/events/" + event + "/seats?size=50").exchange().expectStatus().isOk());
        assertThat(seatMap.findValues("available")).allMatch(JsonNode::asBoolean);
        book(other, event, List.of(chosen.get(0))).expectStatus().isCreated();
    }

    @Test
    void failedBookingIsRolledBack() {
        long event = published();
        long before = jdbc.queryForObject("select count(*) from bookings", Long.class);
        long otherHall = events.hall(1, 1);
        Long foreignSeat = events.seatIds(otherHall).get(0);

        book(customer, event, List.of(seats.get(10), foreignSeat)).expectStatus().isEqualTo(409);

        assertThat(jdbc.queryForObject("select count(*) from bookings", Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from seat_reservations where event_id = ?", Long.class, event)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from tickets where event_id = ?", Long.class, event)).isZero();
    }

    @Test
    void onlyPublishedEventsAreBookable() {
        long draft = events.event(organizer, hall, EventStatus.DRAFT, BigDecimal.TEN);
        book(customer, draft, List.of(seats.get(0))).expectStatus().isNotFound();
        book(customer, 424242, List.of(seats.get(0))).expectStatus().isNotFound();

        long cancelled = events.event(organizer, hall, EventStatus.CANCELLED, BigDecimal.TEN);
        long adminCustomer = users.add(UserRole.CUSTOMER);
        book(adminCustomer, cancelled, List.of(seats.get(0))).expectStatus().isNotFound();

        book(customer, published(), List.of(seats.get(0), seats.get(0))).expectStatus().isBadRequest();
        book(customer, published(), List.of()).expectStatus().isBadRequest();
    }

    @Test
    void accessControl() {
        long event = published();
        long bookingId = body(book(customer, event, List.of(seats.get(30))).expectStatus().isCreated()).get("id").asLong();
        long other = users.add(UserRole.CUSTOMER);

        client.get().uri("/api/bookings/" + bookingId).header("X-User-Id", String.valueOf(other)).exchange().expectStatus().isForbidden();
        client.post().uri("/api/bookings/" + bookingId + "/cancel").header("X-User-Id", String.valueOf(other)).exchange().expectStatus().isForbidden();
        client.get().uri("/api/bookings/" + bookingId).header("X-User-Id", String.valueOf(admin)).exchange().expectStatus().isOk();
        client.get().uri("/api/bookings/" + bookingId).exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/bookings/" + bookingId).header("X-User-Id", "999999").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/bookings/424242").header("X-User-Id", String.valueOf(admin)).exchange().expectStatus().isNotFound();
        client.get().uri("/api/bookings").header("X-User-Id", String.valueOf(organizer)).exchange().expectStatus().isForbidden();

        long intruder = users.add(UserRole.ORGANIZER);
        client.get().uri("/api/events/" + event + "/sales").header("X-User-Id", String.valueOf(intruder)).exchange().expectStatus().isForbidden();
        client.get().uri("/api/events/" + event + "/sales").header("X-User-Id", String.valueOf(admin)).exchange().expectStatus().isOk();
        client.get().uri("/api/events/" + event + "/sales").header("X-User-Id", String.valueOf(customer)).exchange().expectStatus().isForbidden();
    }

    @Test
    void cancellingEventCancelsItsBookings() {
        long event = published();
        long bookingId = body(book(customer, event, List.of(seats.get(40))).expectStatus().isCreated()).get("id").asLong();

        client.post().uri("/api/events/" + event + "/bookings/cancel").header("X-User-Id", String.valueOf(organizer))
                .exchange().expectStatus().isEqualTo(409);

        events.status(event, EventStatus.CANCELLED);
        long intruder = users.add(UserRole.ORGANIZER);
        client.post().uri("/api/events/" + event + "/bookings/cancel").header("X-User-Id", String.valueOf(intruder))
                .exchange().expectStatus().isNotFound();

        client.post().uri("/api/events/" + event + "/bookings/cancel").header("X-User-Id", String.valueOf(organizer))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.cancelled").isEqualTo(1);
        client.post().uri("/api/events/" + event + "/bookings/cancel").header("X-User-Id", String.valueOf(admin))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.cancelled").isEqualTo(0);

        client.get().uri("/api/bookings/" + bookingId).header("X-User-Id", String.valueOf(customer)).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("select count(*) from seat_reservations where event_id = ?", Long.class, event)).isZero();
    }

    @Test
    void pagingIsCappedAndValidated() {
        client.get().uri("/api/tickets?size=500").header("X-User-Id", String.valueOf(customer)).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.size").isEqualTo(50);
        client.get().uri("/api/bookings?page=-1").header("X-User-Id", String.valueOf(customer)).exchange()
                .expectStatus().isBadRequest();
        client.get().uri("/api/events/" + published() + "/seats?page=0&size=20").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content.length()").isEqualTo(20).jsonPath("$.hasNext").isEqualTo(true);
    }

    @Test
    void dependenciesDownGive503() {
        long event = published();
        events.down(true);
        client.get().uri("/api/events/" + event + "/seats").exchange().expectStatus().isEqualTo(503);
        book(customer, event, List.of(seats.get(45))).expectStatus().isEqualTo(503);
        events.down(false);

        users.down(true);
        client.get().uri("/api/bookings").header("X-User-Id", String.valueOf(customer)).exchange().expectStatus().isEqualTo(503);
        client.get().uri("/api/events/" + event + "/seats").exchange().expectStatus().isOk();
    }

    @Test
    void openApiIsServed() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/bookings'].post").exists()
                .jsonPath("$.paths['/api/events/{id}/seats'].get").exists()
                .jsonPath("$.components.securitySchemes['X-User-Id'].in").isEqualTo("header");
    }
}
