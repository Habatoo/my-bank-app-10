package io.github.habatoo.services;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import reactor.core.publisher.Mono;

/**
 * Сервис для управления операциями по переводу денежных средств.
 * <p>
 * Содержит бизнес-логику проверки баланса, верификации счетов
 * и обеспечения атомарности транзакций при межбанковских или внутренних переводах.
 */
public interface TransferService {

    /**
     * Выполняет обработку операции перевода средств между пользователями.
     * <p>
     * Метод реализует следующие этапы:
     * <ul>
     * <li>Проверка существования отправителя и получателя;</li>
     * <li>Валидация достаточности средств на счету отправителя;</li>
     * <li>Списание средств со счета отправителя и зачисление на счет получателя;</li>
     * <li>Сохранение записи о транзакции в историю.</li>
     * </ul>
     *
     * @param senderLogin логин пользователя, инициирующего перевод (отправитель).
     * @param transferDto объект с данными о переводе (логин получателя и сумма).
     * @return {@link Mono}, содержащий {@link OperationResultDto} с результатом операции
     * (успех/ошибка) и данными о созданном переводе.
     */
    Mono<OperationResultDto<TransferDto>> processTransferOperation(String senderLogin, TransferDto transferDto);
}
