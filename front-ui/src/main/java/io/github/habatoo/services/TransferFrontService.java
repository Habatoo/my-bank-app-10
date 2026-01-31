package io.github.habatoo.services;

import io.github.habatoo.dto.TransferDto;
import reactor.core.publisher.Mono;

/**
 * Сервис для осуществления денежных переводов между пользователями системы.
 */
public interface TransferFrontService {

    /**
     * Выполняет процедуру перевода денежных средств на счет другого клиента.
     *
     * @param transferDto объект передачи данных, содержащий идентификатор (логин)
     *                    получателя и сумму перевода.
     * @return {@link Mono}, содержащий строку перенаправления на главную страницу.
     * При успешном завершении добавляется параметр {@code info},
     * при ошибке (например, нехватка средств) — параметр {@code error}.
     */
    Mono<String> sendMoney(TransferDto transferDto);

    /**
     * Выполняет процедуру перевода денежных средств на свой счет.
     *
     * @param transferDto объект передачи данных, содержащий идентификатор (логин)
     *                    получателя и сумму перевода.
     * @return {@link Mono}, содержащий строку перенаправления на главную страницу.
     * При успешном завершении добавляется параметр {@code info},
     * при ошибке (например, нехватка средств) — параметр {@code error}.
     */
    Mono<String> sendMoneyToSelf(TransferDto transferDto);
}
