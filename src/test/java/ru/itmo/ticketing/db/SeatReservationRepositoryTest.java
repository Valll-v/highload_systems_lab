package ru.itmo.ticketing.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.event.Category;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventStatus;
import ru.itmo.ticketing.support.RepositoryTestBase;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.Seat;
import ru.itmo.ticketing.venue.SeatReservation;
import ru.itmo.ticketing.venue.SeatReservationRepository;
import ru.itmo.ticketing.venue.Venue;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatReservationRepositoryTest extends RepositoryTestBase {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SeatReservationRepository reservations;

    private Event event;
    private Booking booking;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        User organizer = em.persist(TestData.user(UserRole.ORGANIZER));
        User customer = em.persist(TestData.user(UserRole.CUSTOMER));
        Category category = em.persist(TestData.category());
        Venue venue = em.persist(TestData.venueWithHall(1, 3));
        Hall hall = venue.getHalls().get(0);
        seat1 = hall.getSeats().get(0);
        seat2 = hall.getSeats().get(1);
        event = em.persist(TestData.event(category, organizer, hall, OffsetDateTime.now().plusDays(1), EventStatus.PUBLISHED));
        booking = em.persist(TestData.booking(customer, event));
        em.flush();
    }

    @Test
    void sameSeatCannotBeReservedTwiceForOneEvent() {
        reservations.saveAndFlush(new SeatReservation(event, seat1, booking));

        assertThatThrownBy(() -> reservations.saveAndFlush(new SeatReservation(event, seat1, booking)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_reservations_event_seat");
    }

    @Test
    void sameSeatCanBeReservedForDifferentEvents() {
        Event another = em.persist(TestData.event(event.getCategory(), event.getOrganizer(), event.getHall(),
                OffsetDateTime.now().plusDays(2), EventStatus.PUBLISHED));
        Booking anotherBooking = em.persist(TestData.booking(booking.getCustomer(), another));

        em.persistAndFlush(new SeatReservation(event, seat1, booking));
        em.persistAndFlush(new SeatReservation(another, seat1, anotherBooking));

        assertThat(reservations.countByEventId(event.getId())).isEqualTo(1);
        assertThat(reservations.countByEventId(another.getId())).isEqualTo(1);
    }

    @Test
    void findReservedSeatIdsReturnsOnlyTakenSeats() {
        em.persistAndFlush(new SeatReservation(event, seat1, booking));

        assertThat(reservations.findReservedSeatIds(event.getId())).containsExactly(seat1.getId());
        assertThat(reservations.findReservedSeatIds(event.getId(), List.of(seat1.getId(), seat2.getId())))
                .containsExactly(seat1.getId());
        assertThat(reservations.findReservedSeatIds(event.getId(), List.of(seat2.getId()))).isEmpty();
    }

    @Test
    void deleteByBookingReleasesAllSeatsOfThatBooking() {
        em.persist(new SeatReservation(event, seat1, booking));
        em.persist(new SeatReservation(event, seat2, booking));
        em.flush();

        long deleted = reservations.deleteByBookingId(booking.getId());

        assertThat(deleted).isEqualTo(2);
        assertThat(reservations.countByEventId(event.getId())).isZero();
    }
}
