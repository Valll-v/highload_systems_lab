package ru.itmo.ticketing.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<Boolean> existsByEmailIgnoreCase(String email);

    Flux<User> findAllBy(Pageable pageable);
}
