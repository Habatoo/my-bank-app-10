package io.github.habatoo.services;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Блок внесения и снятия виртуальных денег
 * Состоит из:
 * поля ввода суммы снятия (обязательно для заполнения);
 * кнопок «Положить» и «Снять» (если сумма, которую нужно снять, больше суммы на счёте, то появляется ошибка).
 */
public interface CashService {

    /**
     * Пополнит счет.
     *
     * @param money сумма для пополнения счета.
     * @return
     */
    Mono<BigDecimal> depositMoney(BigDecimal money);

    /**
     * Снять деньги со счета.
     *
     * @param money сумма для снятия со счета.
     * @return
     */
    Mono<BigDecimal> withdrawMoney(BigDecimal money);
}

