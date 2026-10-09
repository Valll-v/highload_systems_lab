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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.event.dto.CategoryRequest;
import ru.itmo.ticketing.event.dto.CategoryResponse;
import ru.itmo.ticketing.user.UserRole;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(service.list().stream().map(CategoryResponse::from).toList());
    }

    @PostMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = CategoryResponse.from(service.create(request.name()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(category.id()).toUri();
        return ResponseEntity.created(location).body(category);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<CategoryResponse> rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(CategoryResponse.from(service.rename(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
