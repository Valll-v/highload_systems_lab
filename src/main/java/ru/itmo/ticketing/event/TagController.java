package ru.itmo.ticketing.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.PageResponse;
import ru.itmo.ticketing.common.Paging;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.event.dto.TagRequest;
import ru.itmo.ticketing.event.dto.TagResponse;
import ru.itmo.ticketing.user.UserRole;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final TagService service;

    @GetMapping
    public ResponseEntity<PageResponse<TagResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Paging.withTotal(service.list(Paging.of(page, size)).map(TagResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(TagResponse.from(service.get(id)));
    }

    @PostMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        TagResponse tag = TagResponse.from(service.create(request.name()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(tag.id()).toUri();
        return ResponseEntity.created(location).body(tag);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<TagResponse> rename(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(TagResponse.from(service.rename(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
