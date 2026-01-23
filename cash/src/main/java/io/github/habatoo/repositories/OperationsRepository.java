package io.github.habatoo.repositories;

import io.github.habatoo.models.Cash;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

/**
 * Реактивный репозиторий для управления сущностями кассовых операций {@link Cash}.
 * <p>
 * Обеспечивает неблокирующий доступ к данным таблицы {@code operations} с использованием R2DBC.
 * Наследует стандартные методы CRUD (создание, чтение, обновление, удаление), адаптированные
 * под работу с реактивными типами {@link reactor.core.publisher.Mono} и {@link reactor.core.publisher.Flux}.
 * </p>
 *
 * @see io.github.habatoo.models.Cash
 * @see org.springframework.data.r2dbc.repository.R2dbcRepository
 */
public interface OperationsRepository extends R2dbcRepository<Cash, UUID> {
}
