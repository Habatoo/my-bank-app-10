package io.github.habatoo.repositories;

import io.github.habatoo.models.Transfer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

/**
 * Репозиторий работы с Transfer.
 */
public interface TransfersRepository extends R2dbcRepository<Transfer, UUID> {
}
