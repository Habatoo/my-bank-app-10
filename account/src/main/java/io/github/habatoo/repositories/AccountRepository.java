package io.github.habatoo.repositories;

import io.github.habatoo.models.Account;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link Account}.
 * <p>
 * Предоставляет неблокирующий доступ к таблице аккаунтов в базе данных,
 * используя реактивный стек Spring Data R2DBC. Поддерживает стандартные
 * CRUD-операции и специфические запросы для поиска финансовых данных.
 * </p>
 */
public interface AccountRepository extends R2dbcRepository<Account, UUID> {

    /**
     * Выполняет поиск финансового счета по идентификатору владельца (пользователя).
     * <p>
     * Данный метод позволяет получить информацию о балансе и состоянии счета,
     * используя внешний ключ {@code user_id}.
     * </p>
     *
     * @param userId уникальный идентификатор пользователя (UUID).
     * @return {@link Mono}, содержащий найденный аккаунт, либо пустой Mono, если счет не найден.
     */
    Mono<Account> findByUserId(UUID userId);
}
