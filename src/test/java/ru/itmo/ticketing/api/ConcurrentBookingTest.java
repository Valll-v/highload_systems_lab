package ru.itmo.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.ticketing.booking.BookingService;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.support.ApiClient;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class ConcurrentBookingTest {

    private static final int CUSTOMERS = 16;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository users;

    @Test
    void onlyOneBookingWinsTheSeat() throws Exception {
        ApiClient api = new ApiClient(mvc, json);
        long admin = api.user("admin@race.test", "ADMIN");
        long organizer = api.user("org@race.test", "ORGANIZER");
        long category = api.category(admin, "Race");
        long hall = api.hall(admin, api.venue(admin, "Arena", "Kazan"), 2, 2);
        long event = api.publishedEvent(organizer, admin, category, hall, "Hot ticket");
        Long seat = api.seats(hall).get(0);

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < CUSTOMERS; i++) {
            customers.add(users.findById(api.user("c" + i + "@race.test", "CUSTOMER")).orElseThrow());
        }

        ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (User customer : customers) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        bookingService.create(customer, event, List.of(seat));
                        success.incrementAndGet();
                    } catch (ConflictException e) {
                        conflicts.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(CUSTOMERS - 1);
        assertThat(jdbc.queryForObject("select count(*) from seat_reservations where event_id = ? and seat_id = ?",
                Long.class, event, seat)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from tickets where event_id = ?", Long.class, event)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from bookings where event_id = ?", Long.class, event)).isEqualTo(1);
    }
}
