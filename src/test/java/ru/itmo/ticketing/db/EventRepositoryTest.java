package ru.itmo.ticketing.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.itmo.ticketing.event.Category;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventRepository;
import ru.itmo.ticketing.event.EventStatus;
import ru.itmo.ticketing.support.RepositoryTestBase;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.Venue;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventRepositoryTest extends RepositoryTestBase {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EventRepository events;

    private User organizer;
    private Category category;
    private Hall hall;
    private OffsetDateTime start;
    private Event published;

    @BeforeEach
    void setUp() {
        organizer = em.persist(TestData.user(UserRole.ORGANIZER));
        category = em.persist(TestData.category());
        Venue venue = em.persist(TestData.venueWithHall(1, 1));
        hall = venue.getHalls().get(0);
        start = OffsetDateTime.now().plusDays(10).withHour(19).withMinute(0).withSecond(0).withNano(0);
        published = em.persist(TestData.event(category, organizer, hall, start, EventStatus.PUBLISHED));
        em.flush();
    }

    @Test
    void overlappingPublishedEventInSameHallIsConflict() {
        assertThat(events.existsHallConflict(hall.getId(), start.plusHours(1), start.plusHours(3), -1L)).isTrue();
        assertThat(events.existsHallConflict(hall.getId(), start.minusHours(1), start.plusMinutes(1), -1L)).isTrue();
    }

    @Test
    void adjacentOrDistantEventIsNotConflict() {
        assertThat(events.existsHallConflict(hall.getId(), start.plusHours(2), start.plusHours(4), -1L)).isFalse();
        assertThat(events.existsHallConflict(hall.getId(), start.minusHours(5), start.minusHours(3), -1L)).isFalse();
    }

    @Test
    void draftInSameSlotIsNotConflictAndEventIgnoresItself() {
        em.persistAndFlush(TestData.event(category, organizer, hall, start, EventStatus.DRAFT));

        assertThat(events.existsHallConflict(hall.getId(), start, start.plusHours(2), published.getId())).isFalse();
    }

    @Test
    void otherHallSameTimeIsNotConflict() {
        Venue other = em.persistAndFlush(TestData.venueWithHall(1, 1));

        assertThat(events.existsHallConflict(other.getHalls().get(0).getId(), start, start.plusHours(2), -1L)).isFalse();
    }

    @Test
    void findPublishedFiltersByStatusCityAndCategory() {
        em.persist(TestData.event(category, organizer, hall, start.plusDays(1), EventStatus.DRAFT));
        Category otherCategory = em.persist(TestData.category());
        em.persist(TestData.event(otherCategory, organizer, hall, start.plusDays(2), EventStatus.PUBLISHED));
        em.flush();

        Page<Event> all = events.findPublished(null, null, null, null, PageRequest.of(0, 50));
        assertThat(all.getContent()).extracting(Event::getStatus).containsOnly(EventStatus.PUBLISHED);

        Page<Event> byCategory = events.findPublished(category.getId(), "tomsk", null, null, PageRequest.of(0, 50));
        assertThat(byCategory.getContent()).extracting(Event::getId).containsExactly(published.getId());

        Page<Event> byDate = events.findPublished(null, null, start.plusDays(1), null, PageRequest.of(0, 50));
        assertThat(byDate.getContent()).extracting(Event::getId).doesNotContain(published.getId());

        assertThat(events.findPublished(null, "nowhere", null, null, PageRequest.of(0, 50))).isEmpty();
    }
}
