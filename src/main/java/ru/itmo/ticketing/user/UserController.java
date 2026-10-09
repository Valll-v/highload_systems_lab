package ru.itmo.ticketing.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.CurrentUser;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.user.dto.CreateUserRequest;
import ru.itmo.ticketing.user.dto.UpdateRoleRequest;
import ru.itmo.ticketing.user.dto.UserResponse;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = UserResponse.from(service.register(request.email(), request.fullName(), request.role()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(user);
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser User user) {
        return UserResponse.from(user);
    }

    @GetMapping
    @RequireRole(UserRole.ADMIN)
    public List<UserResponse> list() {
        return service.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(service.get(id));
    }

    @PatchMapping("/{id}/role")
    @RequireRole(UserRole.ADMIN)
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return UserResponse.from(service.changeRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
