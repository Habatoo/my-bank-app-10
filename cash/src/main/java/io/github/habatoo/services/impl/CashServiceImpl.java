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
        BigDecimal amountChange = cashDto.getAction() == OperationType.PUT
                ? cashDto.getValue()
                : cashDto.getValue().negate();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host("gateway")
                        .path("/api/account/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amountChange)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                })
                .flatMap(result -> getOperationResultDtoMono(login, cashDto, result));
    }

    private Mono<OperationResultDto<CashDto>> getOperationResultDtoMono(
            String login,
            CashDto cashDto,
            OperationResultDto<Void> result) {
        if (!result.isSuccess()) {
            return Mono.just(OperationResultDto.<CashDto>builder()
                    .success(false)
                    .message(result.getMessage())
                    .errorCode(result.getErrorCode())
                    .build());
        }

        return getOperationResultDtoMono(login, cashDto);
    }

    private Mono<OperationResultDto<CashDto>> getOperationResultDtoMono(String login, CashDto cashDto) {
        Cash operation = Cash.builder()
                .username(login)
                .amount(cashDto.getValue())
                .operationType(cashDto.getAction())
                .createdAt(OffsetDateTime.now())
                .build();

        return operationsRepository.save(operation)
                .then(sendCashNotification(login, cashDto))
                .thenReturn(OperationResultDto.<CashDto>builder()
                        .success(true)
                        .data(cashDto)
                        .message("Операция успешно проведена и сохранена")
                        .build());
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

        log.info("Cash Service: Отправка подтверждения операции для {}", login);

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
