package io.github.habatoo.repositories;

import io.github.habatoo.models.Outbox;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Репозиторий работы с Outbox.
 */
public interface OutboxRepository extends R2dbcRepository<Outbox, UUID> {
    Flux<Outbox> findAllByStatus(String status);

    Mono<Void> deleteAllByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);

    @Modifying
    @Query("DELETE FROM outbox WHERE status = :status AND created_at <= :dateTime")
    Mono<Long> deleteByStatusAndCreatedAtBeforeCustom(String status, LocalDateTime dateTime);

    @Modifying
    @Query("UPDATE outbox SET status = :status WHERE id = :id")
    Mono<Void> updateStatus(UUID id, String status);
}
