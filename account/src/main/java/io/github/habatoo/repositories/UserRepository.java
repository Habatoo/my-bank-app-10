package io.github.habatoo.repositories;

import io.github.habatoo.models.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Репозиторий работы с User.
 */
public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByLogin(String login);

    Mono<Boolean> existsByLogin(String login);

    Flux<User> findAllByLoginNot(String login);
}
