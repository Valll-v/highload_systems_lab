package ru.itmo.ticketing.db;

import ru.itmo.ticketing.booking.Booking;
import ru.itmo.ticketing.event.Category;
import ru.itmo.ticketing.event.Event;
import ru.itmo.ticketing.event.EventStatus;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.Venue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

final class TestData {

    private TestData() {
    }

    static Venue venueWithHall(int rows, int seatsPerRow) {
        Venue venue = new Venue("Venue " + UUID.randomUUID(), "Tomsk", "Lenina 1");
        Hall hall = venue.addHall("Main");
        hall.generateSeats(rows, seatsPerRow);
        return venue;
    }

    static Category category() {
        return new Category("Cat " + UUID.randomUUID());
    }

    static Event event(Category category, Long organizerId, Hall hall, OffsetDateTime start, EventStatus status) {
        Event event = new Event("Show", null, category, organizerId, hall, start, start.plusHours(2), new BigDecimal("100.00"));
        event.setStatus(status);
        return event;
    }

    static Booking booking(Long customerId, Event event) {
        return new Booking(customerId, event, event.getPrice());
    }
}
