package io.github.habatoo.repositories;

import io.github.habatoo.models.Account;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Репозиторий работы с Account.
 */
public interface AccountRepository extends R2dbcRepository<Account, UUID> {
    Mono<Account> findByUserId(UUID userId);
}
