package ru.itmo.ticketing.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.itmo.ticketing.user.common.CurrentUser;
import ru.itmo.ticketing.user.common.PageResponse;
import ru.itmo.ticketing.user.common.Paging;
import ru.itmo.ticketing.user.dto.CreateUserRequest;
import ru.itmo.ticketing.user.dto.UpdateRoleRequest;
import ru.itmo.ticketing.user.dto.UserResponse;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    @PostMapping
    public Mono<ResponseEntity<UserResponse>> register(@Valid @RequestBody CreateUserRequest request,
                                                       ServerHttpRequest http) {
        return service.register(request.email(), request.fullName(), request.role())
                .map(user -> {
                    URI location = UriComponentsBuilder.fromUri(http.getURI()).path("/{id}").build(user.getId());
                    return ResponseEntity.created(location).body(UserResponse.from(user));
                });
    }

    @GetMapping("/me")
    public Mono<UserResponse> me(@CurrentUser User user) {
        return Mono.just(UserResponse.from(user));
    }

    @GetMapping
    public Mono<ResponseEntity<PageResponse<UserResponse>>> list(@CurrentUser(UserRole.ADMIN) User admin,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(Paging.of(page, size))
                .map(users -> Paging.withTotal(users.map(UserResponse::from)));
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> get(@CurrentUser(UserRole.ADMIN) User admin, @PathVariable Long id) {
        return service.get(id).map(UserResponse::from);
    }

    @PatchMapping("/{id}/role")
    public Mono<UserResponse> changeRole(@CurrentUser(UserRole.ADMIN) User admin, @PathVariable Long id,
                                         @Valid @RequestBody UpdateRoleRequest request) {
        return service.changeRole(id, request.role()).map(UserResponse::from);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@CurrentUser(UserRole.ADMIN) User admin, @PathVariable Long id) {
        return service.delete(id).thenReturn(ResponseEntity.noContent().build());
    }
}
