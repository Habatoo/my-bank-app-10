package io.github.habatoo.services.impl;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.models.Transfer;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.TransferService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
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
public class TransferServiceImpl implements TransferService {

    private final WebClient webClient;
    private final TransfersRepository transfersRepository;
    private final OutboxClientService outboxClientService;
    private final CircuitBreakerRegistry registry;

    @Override
    public Mono<OperationResultDto<TransferDto>> processTransferOperation(
            String senderLogin,
            TransferDto transferDto) {

        BigDecimal amount = transferDto.getValue();
        String recipientLogin = transferDto.getLogin();

        return withdrawFromSender(senderLogin, amount)
                .flatMap(withdrawRes -> {
                    if (!withdrawRes.isSuccess()) {
                        return Mono.just(handleWithdrawError(withdrawRes));
                    }

                    return depositToRecipient(recipientLogin, amount)
                            .flatMap(depositRes -> handleDepositStep(
                                    depositRes, senderLogin, recipientLogin, amount, transferDto));
                });
    }

    private Mono<OperationResultDto<TransferDto>> handleDepositStep(
            OperationResultDto<Void> depositRes,
            String sender,
            String recipient,
            BigDecimal amount,
            TransferDto dto) {

        if (depositRes.isSuccess()) {
            return saveTransferRecord(sender, recipient, amount, dto);
        }

        log.error("КРИТИЧЕСКАЯ ОШИБКА: Списано у {}, но не зачислено {}. Запуск компенсации...", sender, recipient);

        return compensateWithdrawal(sender, amount)
                .then(saveNotification(sender, amount, EventStatus.FAILURE))
                .then(Mono.just(createDepositErrorResponse()));
    }

    private Mono<OperationResultDto<TransferDto>> saveTransferRecord(
            String sender,
            String target,
            BigDecimal amount,
            TransferDto dto) {

        Transfer transfer = Transfer.builder()
                .senderUsername(sender)
                .targetUsername(target)
                .amount(amount)
                .createdAt(OffsetDateTime.now())
                .build();

        return transfersRepository.save(transfer)
                .then(saveNotification(sender, amount, EventStatus.SUCCESS, target))
                .thenReturn(OperationResultDto.<TransferDto>builder()
                        .success(true)
                        .data(dto)
                        .message("Перевод успешно выполнен")
                        .build());
    }

    /**
     * Универсальный метод сохранения уведомлений через Outbox.
     */
    private Mono<Void> saveNotification(String login, BigDecimal amount, EventStatus status, String... target) {
        String targetName = (target.length > 0) ? target[0] : "получателю";

        String message = (status == EventStatus.SUCCESS)
                ? String.format("Перевод пользователю %s на сумму %.2f руб. завершен успешно.", targetName, amount)
                : String.format("Перевод не удался. Сумма %.2f возвращена на ваш счет.", amount);

        NotificationEvent event = NotificationEvent.builder()
                .username(login)
                .eventType(EventType.TRANSFER)
                .status(status)
                .message(message)
                .sourceService("transfer-service")
                .payload(Map.of(
                        "sender_username", login,
                        "target_username", targetName,
                        "amount", amount,
                        "status", status.name()
                ))
                .build();

        return outboxClientService.saveEvent(event);
    }

    private Mono<Void> compensateWithdrawal(String sender, BigDecimal amount) {
        log.info("Компенсация: Возврат суммы {} пользователю {}", amount, sender);
        return callAccountService(sender, amount)
                .flatMap(res -> {
                    if (!res.isSuccess()) {
                        log.error(
                                "ФАТАЛЬНАЯ ОШИБКА КОМПЕНСАЦИИ: Не удалось вернуть {} пользователю {}", amount, sender);
                    }
                    return Mono.empty();
                });
    }

    private Mono<OperationResultDto<Void>> withdrawFromSender(String login, BigDecimal amount) {
        return callAccountService(login, amount.negate());
    }

    private Mono<OperationResultDto<Void>> depositToRecipient(String login, BigDecimal amount) {
        return callAccountService(login, amount);
    }

    private Mono<OperationResultDto<Void>> callAccountService(String login, BigDecimal amount) {
        CircuitBreaker cb = registry.circuitBreaker("account-service-cb");

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host("gateway")
                        .path("/api/account/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amount)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                })
                .transformDeferred(CircuitBreakerOperator.of(cb));
    }

    private OperationResultDto<TransferDto> handleWithdrawError(OperationResultDto<Void> res) {
        return OperationResultDto.<TransferDto>builder()
                .success(false)
                .message("Ошибка списания: " + res.getMessage())
                .build();
    }

    private OperationResultDto<TransferDto> createDepositErrorResponse() {
        return OperationResultDto.<TransferDto>builder()
                .success(false)
                .message("Не удалось зачислить средства получателю. Деньги возвращены на ваш счет.")
                .build();
    }
}
