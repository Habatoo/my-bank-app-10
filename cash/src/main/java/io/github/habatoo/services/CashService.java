package io.github.habatoo.services;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Реактивный сервис для управления операциями с наличными средствами (инкассация/депозит).
 */
public interface CashService {

    /**
     * Выполняет полный цикл обработки кассовой операции.льном хранилище.
     *
     * @param value    сумма денежных средств для проведения операции.
     * @param action   тип действия (например, "PUT" для внесения или "GET" для снятия).
     * @param currency строковый код валюты (RUB, USD, CNY).
     * @param jwt      объект авторизованного пользователя, содержащий идентификатор (subject)
     * и логин (preferred_username).
     * @return {@link Mono}, эмиттирующий {@link OperationResultDto} с результатом транзакции.
     * В случае успеха возвращает объект {@link CashDto} с данными чека.
     * В случае бизнес-ошибки или недоступности систем возвращает результат с флагом success=false
     * и описанием причины.
     */
    Mono<OperationResultDto<CashDto>> processCashOperation(
            BigDecimal value,
            String action,
            String currency,
            Jwt jwt);
}
