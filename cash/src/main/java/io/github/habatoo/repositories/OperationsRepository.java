package io.github.habatoo.repositories;

import io.github.habatoo.models.Cash;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

/**
 * Репозиторий работы с Cash.
 */
public interface OperationsRepository extends R2dbcRepository<Cash, UUID> {
}
