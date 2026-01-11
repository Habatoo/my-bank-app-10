package io.github.habatoo.services;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Блок перевода денег на счёт другого аккаунта
 * Состоит из:
 * поля выбора аккаунта пользователя (обязательно для выбора);
 * поля ввода суммы перевода (обязательно для заполнения);
 * кнопки «Перевести» (если сумма перевода больше суммы на счёте отправителя, то появляется ошибка).
 */
public interface TransferService {

    /**
     * Отправить деньги на счет другого пользователя.
     *
     * @param money сумма для отправки на другой счета.
     * @return
     */
    Mono<BigDecimal> sendMoney(BigDecimal money);
}
