package ru.itmo.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.ConflictException;
import ru.itmo.ticketing.common.ForbiddenException;
import ru.itmo.ticketing.common.NotFoundException;
import ru.itmo.ticketing.event.dto.EventRequest;
import ru.itmo.ticketing.event.dto.EventResponse;
import ru.itmo.ticketing.event.dto.EventSeatResponse;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.Hall;
import ru.itmo.ticketing.venue.SeatReservationService;
import ru.itmo.ticketing.venue.VenueService;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private static final Set<EventStatus> EDITABLE = EnumSet.of(EventStatus.DRAFT, EventStatus.REJECTED);

    private final EventRepository events;
    private final CategoryService categories;
    private final VenueService venues;
    private final SeatReservationService reservations;
    private final ApplicationEventPublisher publisher;

    public EventResponse create(User organizer, EventRequest request) {
        validateWindow(request);
        Event event = new Event(
                request.title().trim(), request.description(),
                categories.get(request.categoryId()), organizer, venues.getHall(request.hallId()),
                request.startsAt(), request.endsAt(), request.price());
        return EventResponse.from(events.save(event));
    }

    public EventResponse update(User actor, Long id, EventRequest request) {
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
        return EventResponse.from(event);
    }

    public EventResponse submit(User actor, Long id) {
        Event event = getOwned(actor, id);
        transition(event, EDITABLE, EventStatus.PENDING_MODERATION);
        return EventResponse.from(event);
    }

    public EventResponse cancel(User actor, Long id) {
        Event event = get(id);
        if (!event.isOwnedBy(actor) && actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only the organizer or an admin can cancel the event");
        }
        transition(event, EnumSet.of(EventStatus.DRAFT, EventStatus.PENDING_MODERATION, EventStatus.PUBLISHED, EventStatus.REJECTED),
                EventStatus.CANCELLED);
        publisher.publishEvent(new EventCancelledEvent(event.getId()));
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listMine(User organizer) {
        return events.findAllByOrganizerIdOrderByStartsAtDesc(organizer.getId()).stream()
                .map(EventResponse::from).toList();
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
    public List<EventResponse> listPendingModeration() {
        return events.findAllByStatusOrderByCreatedAtAsc(EventStatus.PENDING_MODERATION).stream()
                .map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> search(Long categoryId, String city, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return events.findPublished(categoryId, city, from, to, pageable).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        return events.findById(id).orElseThrow(() -> new NotFoundException("Event", id));
    }

    @Transactional(readOnly = true)
    public EventResponse getVisible(User viewer, Long id) {
        Event event = get(id);
        if (event.getStatus() == EventStatus.PUBLISHED) {
            return EventResponse.from(event);
        }
        if (viewer != null && (event.isOwnedBy(viewer) || viewer.getRole() == UserRole.ADMIN)) {
            return EventResponse.from(event);
        }
        throw new NotFoundException("Event", id);
    }

    @Transactional(readOnly = true)
    public List<EventSeatResponse> seatMap(Long id) {
        Event event = get(id);
        Set<Long> taken = reservations.reservedSeatIds(id);
        return venues.listSeats(event.getHall().getId()).stream()
                .map(s -> new EventSeatResponse(s.getId(), s.getRowNo(), s.getSeatNo(), !taken.contains(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Event getBookable(Long id) {
        Event event = get(id);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ConflictException("Event is not open for booking (status " + event.getStatus() + ")");
        }
        if (!event.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Event has already started");
        }
        return event;
    }

    private Event getOwned(User actor, Long id) {
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
