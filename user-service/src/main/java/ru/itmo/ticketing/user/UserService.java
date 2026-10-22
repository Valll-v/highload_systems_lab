package ru.itmo.ticketing.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.ticketing.user.common.ConflictException;
import ru.itmo.ticketing.user.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;

    public Mono<User> register(String email, String fullName, UserRole role) {
        String normalized = email.trim().toLowerCase();
        return users.existsByEmailIgnoreCase(normalized)
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("User with email " + normalized + " already exists"))
                        : users.save(new User(normalized, fullName.trim(), role == null ? UserRole.CUSTOMER : role)));
    }

    public Mono<User> get(Long id) {
        return users.findById(id).switchIfEmpty(Mono.error(() -> new NotFoundException("User", id)));
    }

    public Mono<Page<User>> list(Pageable pageable) {
        return users.findAllBy(pageable).collectList()
                .zipWith(users.count(), (content, total) -> new PageImpl<>(content, pageable, total));
    }

    public Mono<User> changeRole(Long id, UserRole role) {
        return get(id).flatMap(user -> {
            user.setRole(role);
            return users.save(user);
        });
    }

    public Mono<Void> delete(Long id) {
        return get(id).flatMap(users::delete);
    }
}
