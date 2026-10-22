package ru.itmo.ticketing.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.itmo.ticketing.booking.BookingClient;
import ru.itmo.ticketing.booking.CancelledBookings;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.ForbiddenException;
import ru.itmo.ticketing.common.NotFoundException;
import ru.itmo.ticketing.common.ServiceUnavailableException;
import ru.itmo.ticketing.event.dto.EventRequest;
import ru.itmo.ticketing.event.dto.EventResponse;
import ru.itmo.ticketing.user.UserInfo;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.VenueService;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private static final Set<EventStatus> EDITABLE = EnumSet.of(EventStatus.DRAFT, EventStatus.REJECTED);

    private final EventRepository events;
    private final CategoryService categories;
    private final TagService tags;
    private final VenueService venues;
    private final BookingClient bookings;

    public EventResponse create(UserInfo organizer, EventRequest request) {
        validateWindow(request);
        Event event = new Event(
                request.title().trim(), request.description(),
                categories.get(request.categoryId()), organizer.id(), venues.getHall(request.hallId()),
                request.startsAt(), request.endsAt(), request.price());
        event.setTags(tags.getAll(request.tagIds()));
        return EventResponse.from(events.save(event));
    }

    public EventResponse update(UserInfo actor, Long id, EventRequest request) {
        Event event = getOwned(actor, id);
        if (!EDITABLE.contains(event.getStatus())) {
            throw new ConflictException("Event can be edited only in DRAFT or REJECTED status, current: " + event.getStatus());
        }
        validateWindow(request);
        Hall hall = venues.getHall(request.hallId());
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setCategory(categories.get(request.categoryId()));
        event.setHall(hall);
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setPrice(request.price());
        event.setTags(tags.getAll(request.tagIds()));
        return EventResponse.from(event);
    }

    public EventResponse submit(UserInfo actor, Long id) {
        Event event = getOwned(actor, id);
        transition(event, EDITABLE, EventStatus.PENDING_MODERATION);
        return EventResponse.from(event);
    }

    /**
     * Статус меняется в своей транзакции, брони отменяет booking-service уже после коммита:
     * он проверит, что мероприятие и правда CANCELLED. Если booking-service лежит, мероприятие
     * всё равно отменено, вызов нужно повторить руками (POST /api/events/{id}/bookings/cancel).
     */
    public EventResponse cancel(UserInfo actor, Long id) {
        Event event = get(id);
        if (!event.isOwnedBy(actor) && actor.role() != UserRole.ADMIN) {
            throw new ForbiddenException("Only the organizer or an admin can cancel the event");
        }
        transition(event, EnumSet.of(EventStatus.DRAFT, EventStatus.PENDING_MODERATION, EventStatus.PUBLISHED, EventStatus.REJECTED),
                EventStatus.CANCELLED);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cancelBookings(event.getId(), actor.id());
            }
        });
        return EventResponse.from(event);
    }

    private void cancelBookings(Long eventId, Long actorId) {
        try {
            CancelledBookings result = bookings.cancelByEvent(eventId, actorId);
            log.info("event {} cancelled, booking-service cancelled {} bookings", eventId, result.cancelled());
        } catch (ServiceUnavailableException e) {
            log.warn("event {} is cancelled, but booking-service is unavailable: "
                    + "repeat POST /api/events/{}/bookings/cancel later", eventId, eventId);
        }
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listMine(UserInfo organizer, Pageable pageable) {
        return events.findAllByOrganizerId(organizer.id(), pageable).map(EventResponse::from);
    }

    public EventResponse publish(Long id) {
        Event event = get(id);
        transition(event, EnumSet.of(EventStatus.PENDING_MODERATION), EventStatus.PUBLISHED);
        if (events.existsHallConflict(event.getHall().getId(), event.getStartsAt(), event.getEndsAt(), event.getId())) {
            throw new ConflictException("Hall is already booked by another published event in this time window");
        }
        return EventResponse.from(event);
    }

    public EventResponse reject(Long id) {
        Event event = get(id);
        transition(event, EnumSet.of(EventStatus.PENDING_MODERATION), EventStatus.REJECTED);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listPendingModeration(Pageable pageable) {
        return events.findAllByStatus(EventStatus.PENDING_MODERATION, pageable).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public Slice<EventResponse> search(Long categoryId, String city, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        String normalizedCity = city == null || city.isBlank() ? null : city.trim().toLowerCase();
        return events.findPublished(categoryId, normalizedCity, from, to, pageable).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        return events.findById(id).orElseThrow(() -> new NotFoundException("Event", id));
    }

    @Transactional(readOnly = true)
    public EventResponse getVisible(UserInfo viewer, Long id) {
        Event event = get(id);
        if (event.getStatus() == EventStatus.PUBLISHED) {
            return EventResponse.from(event);
        }
        if (viewer != null && (event.isOwnedBy(viewer) || viewer.role() == UserRole.ADMIN)) {
            return EventResponse.from(event);
        }
        throw new NotFoundException("Event", id);
    }

    private Event getOwned(UserInfo actor, Long id) {
        Event event = get(id);
        if (!event.isOwnedBy(actor)) {
            throw new ForbiddenException("Event " + id + " belongs to another organizer");
        }
        return event;
    }

    private static void transition(Event event, Set<EventStatus> from, EventStatus to) {
        if (!from.contains(event.getStatus())) {
            throw new ConflictException("Cannot move event from " + event.getStatus() + " to " + to);
        }
        event.setStatus(to);
    }

    private static void validateWindow(EventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }
}
