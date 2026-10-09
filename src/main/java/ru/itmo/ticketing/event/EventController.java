package ru.itmo.ticketing.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.event.dto.EventRequest;
import ru.itmo.ticketing.event.dto.EventResponse;
import ru.itmo.ticketing.event.dto.EventSeatResponse;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.user.UserRepository;
import ru.itmo.ticketing.user.UserRole;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final UserRepository users;

    @GetMapping
    public ResponseEntity<Page<EventResponse>> search(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("startsAt"));
        return ResponseEntity.ok(service.search(categoryId, city, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable Long id,
                             @RequestHeader(value = "X-User-Id", required = false) Long viewerId) {
        User viewer = viewerId == null ? null : users.findById(viewerId).orElse(null);
        return ResponseEntity.ok(service.getVisible(viewer, id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<EventSeatResponse>> seats(@PathVariable Long id) {
        return ResponseEntity.ok(service.seatMap(id));
    }

    @GetMapping("/my")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<List<EventResponse>> getMyEvents(@CurrentUser User organizer) {
        return ResponseEntity.ok(service.listMine(organizer));
    }

    @PostMapping
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> create(@CurrentUser User organizer, @Valid @RequestBody EventRequest request) {
        EventResponse event = service.create(organizer, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(event.id()).toUri();
        return ResponseEntity.created(location).body(event);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> update(@CurrentUser User organizer, @PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(service.update(organizer, id, request));
    }

    @PostMapping("/{id}/submit")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> submit(@CurrentUser User organizer, @PathVariable Long id) {
        return ResponseEntity.ok(service.submit(organizer, id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.ORGANIZER, UserRole.ADMIN})
    public ResponseEntity<EventResponse> cancel(@CurrentUser User actor, @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(actor, id));
    }

    @GetMapping("/moderation")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<List<EventResponse>> moderationQueue() {
        return ResponseEntity.ok(service.listPendingModeration());
    }

    @PostMapping("/{id}/publish")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<EventResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(service.publish(id));
    }

    @PostMapping("/{id}/reject")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<EventResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.reject(id));
    }
}
