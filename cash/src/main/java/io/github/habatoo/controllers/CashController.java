package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
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
import java.time.LocalDateTime;
import java.util.UUID;

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
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('CASH_ACCESS')")
    public Mono<OperationResultDto<CashDto>> updateBalance(
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") String action,
            @RequestParam("currency") String currencyStr,
            @AuthenticationPrincipal Jwt jwt) {

        String login = jwt.getClaimAsString("preferred_username");
        String userIdStr = jwt.getSubject();

        log.info("Запрос на операцию {} для пользователя {} на сумму {}", action, login, value);

        CashDto.CashDtoBuilder cashDtoBuilder = CashDto.builder()
                .userId(UUID.fromString(userIdStr))
                .value(value)
                .createdAt(LocalDateTime.now());

        OperationType operationType;
        Currency currency;
        try {
            operationType = OperationType.valueOf(action.toUpperCase());
            currency = Currency.valueOf(currencyStr);
        } catch (IllegalArgumentException e) {
            return Mono.just(OperationResultDto.<CashDto>builder()
                    .success(false)
                    .data(cashDtoBuilder.build())
                    .message("Неверный формат параметров: " + e.getMessage())
                    .build());
        }

        CashDto cashDto = cashDtoBuilder
                .action(operationType)
                .currency(currency)
                .build();

        return cashService.processCashOperation(login, cashDto);
    }
}
