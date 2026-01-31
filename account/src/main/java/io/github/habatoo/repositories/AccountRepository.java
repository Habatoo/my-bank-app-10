package io.github.habatoo.repositories;

import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.models.Account;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link Account}.
 */
public interface AccountRepository extends R2dbcRepository<Account, UUID> {

    /**
     * Выполняет поиск финансового счета по идентификатору владельца (пользователя).
     *
     * @param userId уникальный идентификатор пользователя (UUID).
     * @param currency валюта счета.
     * @return {@link Mono}, содержащий найденный аккаунт, либо пустой Mono, если счет не найден.
     */
    Mono<Account> findByUserIdAndCurrency(UUID userId, Currency currency);

    /**
     * Выполняет поиск счетов по идентификатору владельца (пользователя).
     *
     * @param userId уникальный идентификатор пользователя (UUID).
     * @return {@link Flux}, содержащий список счетов.
     */
    Flux<Account> findAllByUserId(UUID userId);
}
