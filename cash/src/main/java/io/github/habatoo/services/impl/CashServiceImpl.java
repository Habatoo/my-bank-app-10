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
import io.github.habatoo.services.NotificationClientService;
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
    private final NotificationClientService notificationClient;

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
     * Завершение операции с механизмом компенсации.
     */
    private Mono<OperationResultDto<CashDto>> completeCashOperation(
            String login,
            CashDto cashDto,
            BigDecimal amountChange) {
        Cash operation = buildCashEntity(login, cashDto);

        return operationsRepository.save(operation)
                .then(sendCashNotification(login, cashDto, EventStatus.SUCCESS))
                .thenReturn(buildSuccessResponse(cashDto))
                .onErrorResume(e -> {
                    log.error("ОШИБКА БД в CashService для {}. Запуск компенсации в AccountService...", login);
                    return compensateAccountUpdate(login, amountChange)
                            .then(sendCashNotification(login, cashDto, EventStatus.FAILURE))
                            .then(Mono.just(buildDatabaseErrorResponse(cashDto)));
                });
    }

    /**
     * Метод компенсации: инвертирует операцию в Account Service.
     */
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

    private OperationResultDto<CashDto> buildDatabaseErrorResponse(CashDto cashDto) {
        return OperationResultDto.<CashDto>builder()
                .success(false)
                .data(cashDto)
                .message("Ошибка сохранения операции. Средства возвращены на счет.")
                .build();
    }

    /**
     * Обновленный метод отправки уведомлений (принимает статус).
     */
    private Mono<Void> sendCashNotification(String login, CashDto dto, EventStatus status) {
        NotificationEvent event = getNotificationEvent(login, dto, status);

        return notificationClient.send(event)
                .doOnError(e -> log.error("Не удалось отправить уведомление для {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private NotificationEvent getNotificationEvent(String login, CashDto dto, EventStatus status) {
        boolean isDeposit = dto.getAction() == OperationType.PUT;
        String actionName = isDeposit ? "Внесение" : "Снятие";

        String message = status == EventStatus.SUCCESS
                ? String.format("%s наличных на сумму %.2f руб. завершено успешно.", actionName, dto.getValue())
                : String.format("Ошибка при %s наличных. Средства возвращены на счет.", actionName.toLowerCase());

        return NotificationEvent.builder()
                .username(login)
                .eventType(isDeposit ? EventType.DEPOSIT : EventType.WITHDRAW)
                .status(status)
                .message(message)
                .sourceService("cash-service")
                .payload(Map.of("amount", dto.getValue()))
                .build();
    }

    /**
     * Вычисляет дельту изменения баланса (отрицательная при снятии).
     */
    private BigDecimal calculateAmountChange(CashDto cashDto) {
        return cashDto.getAction() == OperationType.PUT
                ? cashDto.getValue()
                : cashDto.getValue().negate();
    }

    /**
     * Вызов внешнего сервиса аккаунтов через шлюз.
     */
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

    /**
     * Завершение операции: сохранение в БД и отправка уведомления.
     */
    private Mono<OperationResultDto<CashDto>> completeCashOperation(String login, CashDto cashDto) {
        Cash operation = buildCashEntity(login, cashDto);

        return operationsRepository.save(operation)
                .then(sendCashNotification(login, cashDto))
                .thenReturn(buildSuccessResponse(cashDto));
    }

    /**
     * Создание сущности для записи в историю операций.
     */
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

    private Mono<Void> sendCashNotification(String login, CashDto dto) {
        NotificationEvent event = getNotificationEvent(login, dto);

        return notificationClient.send(event)
                .doOnError(e -> log.error("Не удалось отправить уведомление для {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private NotificationEvent getNotificationEvent(String login, CashDto dto) {
        boolean isDeposit = dto.getAction() == OperationType.PUT;
        String actionName = isDeposit ? "Внесение" : "Снятие";
        String message = String.format("%s наличных на сумму %.2f руб. завершено успешно.",
                actionName, dto.getValue());

        log.info("Cash Service: Подготовка уведомления для {}", login);

        return NotificationEvent.builder()
                .username(login)
                .eventType(isDeposit ? EventType.DEPOSIT : EventType.WITHDRAW)
                .status(EventStatus.SUCCESS)
                .message(message)
                .sourceService("cash-service")
                .payload(Map.of(
                        "amount", dto.getValue(),
                        "operationType", actionName,
                        "currency", "RUB"
                ))
                .build();
    }
}
