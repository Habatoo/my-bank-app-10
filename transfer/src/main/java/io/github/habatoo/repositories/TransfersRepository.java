package io.github.habatoo.repositories;

import io.github.habatoo.models.Transfer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

/**
 * Репозиторий для работы с сущностью {@link Transfer}.
 * Обеспечивает реактивный доступ к данным о денежных переводах в базе данных PostgreSQL
 * с использованием R2DBC.
 */
public interface TransfersRepository extends R2dbcRepository<Transfer, UUID> {
}
