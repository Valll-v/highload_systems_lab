package ru.itmo.ticketing.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import ru.itmo.ticketing.support.RepositoryTestBase;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.Seat;
import ru.itmo.ticketing.venue.SeatRepository;
import ru.itmo.ticketing.venue.Venue;
import ru.itmo.ticketing.venue.VenueRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VenueRepositoryTest extends RepositoryTestBase {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VenueRepository venues;

    @Autowired
    private SeatRepository seats;

    @Test
    void savingVenueCascadesToHallsAndSeats() {
        Venue venue = venues.saveAndFlush(TestData.venueWithHall(3, 4));
        em.clear();

        Venue loaded = venues.findById(venue.getId()).orElseThrow();
        Hall hall = loaded.getHalls().get(0);
        List<Seat> hallSeats = seats.findAllByHallIdOrderByRowNoAscSeatNoAsc(hall.getId());

        assertThat(hallSeats).hasSize(12);
        assertThat(hallSeats.get(0).getRowNo()).isEqualTo(1);
        assertThat(hallSeats.get(0).getSeatNo()).isEqualTo(1);
        assertThat(hallSeats.get(11).getRowNo()).isEqualTo(3);
        assertThat(hallSeats.get(11).getSeatNo()).isEqualTo(4);
    }

    @Test
    void deletingVenueRemovesHallsAndSeats() {
        Venue venue = venues.saveAndFlush(TestData.venueWithHall(2, 2));
        Long hallId = venue.getHalls().get(0).getId();
        em.clear();

        venues.deleteById(venue.getId());
        venues.flush();

        assertThat(seats.findAllByHallIdOrderByRowNoAscSeatNoAsc(hallId)).isEmpty();
        assertThat(em.getEntityManager()
                .createQuery("select count(h) from Hall h where h.id = :id", Long.class)
                .setParameter("id", hallId).getSingleResult()).isZero();
    }

    @Test
    void hallNameIsUniqueWithinVenue() {
        Venue venue = venues.saveAndFlush(TestData.venueWithHall(1, 1));
        venue.addHall("Main");

        assertThatThrownBy(() -> venues.saveAndFlush(venue))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_halls_venue_name");
    }

    @Test
    void lockQueryReturnsSeatsOrderedById() {
        Venue venue = venues.saveAndFlush(TestData.venueWithHall(1, 5));
        List<Long> ids = venue.getHalls().get(0).getSeats().stream().map(Seat::getId).toList();

        List<Seat> locked = seats.lockAllByIdIn(List.of(ids.get(4), ids.get(0), ids.get(2)));

        assertThat(locked).extracting(Seat::getId).containsExactly(ids.get(0), ids.get(2), ids.get(4));
    }
}
