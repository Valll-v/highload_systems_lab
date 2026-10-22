package ru.itmo.ticketing.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.itmo.ticketing.booking.client.EventStatus;
import ru.itmo.ticketing.booking.client.UserRole;
import ru.itmo.ticketing.booking.common.ConflictException;
import ru.itmo.ticketing.booking.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Места лежат в другой базе, блокировать их строки нельзя. Одно место на мероприятие
 * защищает уникальный индекс seat_reservations(event_id, seat_id).
 */
class ConcurrentBookingTest extends IntegrationTestBase {

    private static final int CUSTOMERS = 16;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void onlyOneBookingWinsTheSeat() throws Exception {
        long organizer = users.add(UserRole.ORGANIZER);
        long hall = events.hall(2, 2);
        long event = events.event(organizer, hall, EventStatus.PUBLISHED, new BigDecimal("100.00"));
        Long seat = events.seatIds(hall).get(0);

        List<Long> customers = new ArrayList<>();
        for (int i = 0; i < CUSTOMERS; i++) {
            customers.add(users.add(UserRole.CUSTOMER));
        }

        ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Long customer : customers) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        bookingService.create(users.get(customer), event, List.of(seat));
                        success.incrementAndGet();
                    } catch (ConflictException | DataIntegrityViolationException e) {
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
