package ru.itmo.ticketing.event;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.CurrentUserResolver;
import ru.itmo.ticketing.common.PageResponse;
import ru.itmo.ticketing.common.Paging;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.common.SliceResponse;
import ru.itmo.ticketing.event.dto.EventRequest;
import ru.itmo.ticketing.event.dto.EventResponse;
import ru.itmo.ticketing.event.dto.EventSeatResponse;
import ru.itmo.ticketing.user.UserInfo;
import ru.itmo.ticketing.user.UserRole;

import java.net.URI;
import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final CurrentUserResolver currentUser;

    @GetMapping
    public ResponseEntity<SliceResponse<EventResponse>> search(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by("startsAt", "id"));
        return ResponseEntity.ok(SliceResponse.from(service.search(categoryId, city, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @Parameter(in = ParameterIn.HEADER, name = CurrentUserResolver.HEADER,
            description = "необязательно: организатор и админ видят и неопубликованное мероприятие")
    public ResponseEntity<EventResponse> get(@PathVariable Long id, HttpServletRequest request) {
        UserInfo viewer = currentUser.optional(request).orElse(null);
        return ResponseEntity.ok(service.getVisible(viewer, id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<SliceResponse<EventSeatResponse>> seats(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "50") int size) {
        var pageable = Paging.of(page, size, Sort.by("rowNo", "seatNo"));
        return ResponseEntity.ok(SliceResponse.from(service.seatMap(id, pageable)));
    }

    @GetMapping("/my")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<PageResponse<EventResponse>> getMyEvents(@CurrentUser UserInfo organizer,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "startsAt"));
        return Paging.withTotal(service.listMine(organizer, pageable));
    }

    @PostMapping
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> create(@CurrentUser UserInfo organizer, @Valid @RequestBody EventRequest request) {
        EventResponse event = service.create(organizer, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(event.id()).toUri();
        return ResponseEntity.created(location).body(event);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> update(@CurrentUser UserInfo organizer, @PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(service.update(organizer, id, request));
    }

    @PostMapping("/{id}/submit")
    @RequireRole(UserRole.ORGANIZER)
    public ResponseEntity<EventResponse> submit(@CurrentUser UserInfo organizer, @PathVariable Long id) {
        return ResponseEntity.ok(service.submit(organizer, id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.ORGANIZER, UserRole.ADMIN})
    public ResponseEntity<EventResponse> cancel(@CurrentUser UserInfo actor, @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(actor, id));
    }

    @GetMapping("/moderation")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<PageResponse<EventResponse>> moderationQueue(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return Paging.withTotal(service.listPendingModeration(Paging.of(page, size, Sort.by("createdAt", "id"))));
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
