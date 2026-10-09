package ru.itmo.ticketing.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.ticketing.support.IntegrationTestBase;
import ru.itmo.ticketing.venue.Seat;
import ru.itmo.ticketing.venue.SeatRepository;
import ru.itmo.ticketing.venue.Venue;
import ru.itmo.ticketing.venue.VenueRepository;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatLockTest extends IntegrationTestBase {

    @Autowired
    private VenueRepository venues;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void secondTransactionWaitsForRowLockUntilFirstCommits() throws Exception {
        Venue venue = venues.save(TestData.venueWithHall(1, 1));
        Long seatId = venue.getHalls().get(0).getSeats().get(0).getId();

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> holder = pool.submit(() -> tx.execute(status -> {
                seats.lockAllByIdIn(List.of(seatId));
                locked.countDown();
                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));

            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                jdbc.execute("set local lock_timeout = '500ms'");
                seats.lockAllByIdIn(List.of(seatId));
            })).isInstanceOfAny(PessimisticLockingFailureException.class, QueryTimeoutException.class);

            release.countDown();
            holder.get(10, TimeUnit.SECONDS);

            List<Seat> afterCommit = tx.execute(status -> seats.lockAllByIdIn(List.of(seatId)));
            assertThat(afterCommit).hasSize(1);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}
