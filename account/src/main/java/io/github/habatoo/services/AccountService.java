package io.github.habatoo.services;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Сервис для управления финансовыми счетами пользователей.
 * <p>
 * Содержит бизнес-логику для получения информации о состоянии счетов
 * и выполнения транзакционных операций по изменению баланса.
 * </p>
 */
public interface AccountService {

    /**
     * Получает полную информацию об аккаунте по логину пользователя.
     * <p>
     * Метод агрегирует данные профиля и текущее состояние счета.
     * </p>
     *
     * @param login уникальный логин пользователя.
     * @param currency текущая валюты.
     * @return {@link Mono}, содержащий детальную информацию об аккаунте {@link AccountFullResponseDto}.
     */
    Mono<AccountFullResponseDto> getByLogin(String login, String currency);

    /**
     * Получает список кратких данных всех аккаунтов, за исключением текущего.
     * <p>
     * Метод используется для формирования списка доступных получателей перевода,
     * исключая самого отправителя из выборки.
     * </p>
     *
     * @param currentLogin логин текущего авторизованного пользователя.
     * @return {@link Flux} с объектами {@link AccountShortDto} других пользователей.
     */
    Flux<AccountShortDto> getOtherAccounts(String currentLogin);

    /**
     * Выполняет операцию изменения баланса пользователя.
     * <p>
     * Метод позволяет как начислять средства (положительная дельта),
     * так и списывать их (отрицательная дельта). Должен обеспечивать
     * атомарность и проверку достаточности средств при списании.
     * </p>
     *
     * @param login    логин пользователя, чей счет подлежит изменению.
     * @param delta    сумма изменения баланса.
     * @param currency валюта.
     * @return {@link Mono} с результатом операции {@link OperationResultDto},
     * содержащим статус успеха и информационное сообщение.
     */
    Mono<OperationResultDto<Void>> changeBalance(String login, BigDecimal delta, String currency);
}
