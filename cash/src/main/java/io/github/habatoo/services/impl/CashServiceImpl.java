package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.models.Cash;
import io.github.habatoo.repositories.OperationsRepository;
import io.github.habatoo.services.CashService;
import io.github.habatoo.services.OutboxClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final WebClient webClient;
    private final OperationsRepository operationsRepository;
    private final OutboxClientService outboxClientService;

    @Override
    public Mono<OperationResultDto<CashDto>> processCashOperation(String login, CashDto cashDto) {
        BigDecimal amountChange = calculateAmountChange(cashDto);

        return updateAccountBalance(login, amountChange)
                .flatMap(cashRes -> {
                    if (!cashRes.isSuccess()) {
                        return Mono.just(handleAccountError(cashRes));
                    }
                    return completeCashOperation(login, cashDto, amountChange);
                });
    }

    /**
     * Завершение операции: сохранение в локальную БД и фиксация события в Outbox.
     * В случае ошибки БД запускается компенсация во внешнем сервисе.
     */
    private Mono<OperationResultDto<CashDto>> completeCashOperation(String login, CashDto cashDto, BigDecimal amountChange) {
        Cash operation = buildCashEntity(login, cashDto);

        return operationsRepository.save(operation)
                .then(saveNotification(login, cashDto, EventStatus.SUCCESS))
                .thenReturn(buildSuccessResponse(cashDto))
                .onErrorResume(e -> {
                    log.error("ОШИБКА БД в CashService для {}. Запуск компенсации...", login);
                    return compensateAccountUpdate(login, amountChange)
                            .then(saveNotification(login, cashDto, EventStatus.FAILURE))
                            .then(Mono.just(buildDatabaseErrorResponse(cashDto)));
                });
    }

    private Mono<Void> compensateAccountUpdate(String login, BigDecimal originalAmountChange) {
        BigDecimal compensationAmount = originalAmountChange.negate();
        log.info("Компенсация: Возврат суммы {} для пользователя {}", compensationAmount, login);

        return updateAccountBalance(login, compensationAmount)
                .flatMap(res -> {
                    if (!res.isSuccess()) {
                        log.error("ФАТАЛЬНАЯ ОШИБКА: Не удалось компенсировать баланс для {}", login);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> saveNotification(String login, CashDto dto, EventStatus status) {
        NotificationEvent event = buildNotificationEvent(login, dto, status);
        return outboxClientService.saveEvent(event);
    }

    private NotificationEvent buildNotificationEvent(String login, CashDto dto, EventStatus status) {
        boolean isDeposit = dto.getAction() == OperationType.PUT;
        String actionName = isDeposit ? "Внесение" : "Снятие";

        String message = getMessage(dto, status, actionName);

        return NotificationEvent.builder()
                .username(login)
                .eventType(isDeposit ? EventType.DEPOSIT : EventType.WITHDRAW)
                .status(status)
                .message(message)
                .sourceService("cash-service")
                .payload(Map.of(
                        "amount", dto.getValue(),
                        "operationType", actionName
                ))
                .build();
    }

    private String getMessage(CashDto dto, EventStatus status, String actionName) {
        return (status == EventStatus.SUCCESS)
                ? String.format("%s наличных на сумму %.2f руб. завершено успешно.", actionName, dto.getValue())
                : String.format("Ошибка при %s наличных. Средства возвращены на счет.", actionName.toLowerCase());
    }

    private BigDecimal calculateAmountChange(CashDto cashDto) {
        return cashDto.getAction() == OperationType.PUT
                ? cashDto.getValue()
                : cashDto.getValue().negate();
    }

    private Mono<OperationResultDto<Void>> updateAccountBalance(String login, BigDecimal amount) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host("gateway")
                        .path("/api/account/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amount)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                });
    }

    private Cash buildCashEntity(String login, CashDto cashDto) {
        return Cash.builder()
                .username(login)
                .amount(cashDto.getValue())
                .operationType(cashDto.getAction())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private OperationResultDto<CashDto> handleAccountError(OperationResultDto<Void> result) {
        return OperationResultDto.<CashDto>builder()
                .success(false)
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .build();
    }

    private OperationResultDto<CashDto> buildSuccessResponse(CashDto cashDto) {
        return OperationResultDto.<CashDto>builder()
                .success(true)
                .data(cashDto)
                .message("Операция успешно проведена и сохранена")
                .build();
    }

    private OperationResultDto<CashDto> buildDatabaseErrorResponse(CashDto cashDto) {
        return OperationResultDto.<CashDto>builder()
                .success(false)
                .data(cashDto)
                .message("Ошибка сохранения операции. Средства возвращены на счет.")
                .build();
    }
}
