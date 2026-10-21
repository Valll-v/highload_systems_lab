package ru.itmo.ticketing.db;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.itmo.ticketing.event.Category;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventStatus;
import ru.itmo.ticketing.event.Tag;
import ru.itmo.ticketing.support.RepositoryTestBase;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Venue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityValidationTest extends RepositoryTestBase {

    @Autowired
    private TestEntityManager em;

    @Test
    void userWithBadEmailIsRejectedBeforeInsert() {
        assertThatThrownBy(() -> em.persistAndFlush(new User("not-an-email", "Name", UserRole.CUSTOMER)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void blankNamesAreRejected() {
        assertThatThrownBy(() -> em.persistAndFlush(new Venue(" ", "City", "Address")))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> em.persistAndFlush(new Category("")))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> em.persistAndFlush(new Tag("x".repeat(65))))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void eventWindowAndPriceAreChecked() {
        User organizer = em.persist(TestData.user(UserRole.ORGANIZER));
        Category category = em.persist(TestData.category());
        Venue venue = em.persist(TestData.venueWithHall(1, 1));
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        Event backwards = new Event("Show", null, category, organizer, venue.getHalls().get(0),
                start, start.minusHours(1), new BigDecimal("10.00"));
        assertThatThrownBy(() -> em.persistAndFlush(backwards))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("endsAt must be after startsAt");

        Event negative = TestData.event(category, organizer, venue.getHalls().get(0), start, EventStatus.DRAFT);
        negative.setPrice(new BigDecimal("-1.00"));
        assertThatThrownBy(() -> em.persistAndFlush(negative))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("price");
    }
}
