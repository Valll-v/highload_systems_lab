package ru.itmo.ticketing.event;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.event.dto.EventRequest;
import ru.itmo.ticketing.event.dto.EventResponse;
import ru.itmo.ticketing.event.dto.EventSeatResponse;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRepository;
import ru.itmo.ticketing.user.UserRole;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final UserRepository users;

    public EventController(EventService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    public Page<EventResponse> search(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("startsAt"));
        return service.search(categoryId, city, from, to, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id,
                             @RequestHeader(value = "X-User-Id", required = false) Long viewerId) {
        User viewer = viewerId == null ? null : users.findById(viewerId).orElse(null);
        return service.getVisible(viewer, id);
    }

    @GetMapping("/{id}/seats")
    public List<EventSeatResponse> seats(@PathVariable Long id) {
        return service.seatMap(id);
    }

    @GetMapping("/mine")
    @RequireRole(UserRole.ORGANIZER)
    public List<EventResponse> mine(@CurrentUser User organizer) {
        return service.listMine(organizer);
    }

    @PostMapping
    @RequireRole(UserRole.ORGANIZER)
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@CurrentUser User organizer, @Valid @RequestBody EventRequest request) {
        return service.create(organizer, request);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ORGANIZER)
    public EventResponse update(@CurrentUser User organizer, @PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return service.update(organizer, id, request);
    }

    @PostMapping("/{id}/submit")
    @RequireRole(UserRole.ORGANIZER)
    public EventResponse submit(@CurrentUser User organizer, @PathVariable Long id) {
        return service.submit(organizer, id);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.ORGANIZER, UserRole.ADMIN})
    public EventResponse cancel(@CurrentUser User actor, @PathVariable Long id) {
        return service.cancel(actor, id);
    }

    @GetMapping("/moderation")
    @RequireRole(UserRole.ADMIN)
    public List<EventResponse> moderationQueue() {
        return service.listPendingModeration();
    }

    @PostMapping("/{id}/publish")
    @RequireRole(UserRole.ADMIN)
    public EventResponse publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/reject")
    @RequireRole(UserRole.ADMIN)
    public EventResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }
}
