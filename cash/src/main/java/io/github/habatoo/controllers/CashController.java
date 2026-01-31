package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.services.CashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Контроллер для работы с наличными операциями (пополнение и снятие).
 * <p>
 * Предоставляет API для внесения и списания средств с личного счета пользователя.
 * Все операции проверяются на соответствие ролям и привязываются к текущему
 * авторизованному пользователю через JWT.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    /**
     * Выполняет операцию по изменению баланса (депозит или снятие).
     * <p>
     * Метод извлекает идентификатор пользователя (subject) и логин из JWT-токена,
     * формирует объект {@link CashDto} и передает его в сервисную логику.
     * </p>
     *
     * @param value  сумма операции.
     * @param action тип операции (например, "DEPOSIT" или "WITHDRAW").
     * @param jwt    объект авторизованного пользователя, содержащий данные токена.
     * @return {@link Mono} с результатом операции {@link OperationResultDto},
     * содержащим данные о транзакции {@link CashDto}.
     */
    @PostMapping("/cash")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'CASH_ACCESS')")
    public Mono<OperationResultDto<CashDto>> updateBalance(
            @RequestParam BigDecimal value,
            @RequestParam String action,
            @RequestParam String currency,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("CASH API: Получен запрос от {} на {} {} {}",
                jwt.getClaimAsString("preferred_username"), action, value, currency);

        return cashService.processCashOperation(value, action, currency, jwt);
    }
}
