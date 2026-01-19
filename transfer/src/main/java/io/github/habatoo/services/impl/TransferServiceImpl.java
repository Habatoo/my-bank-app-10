package io.github.habatoo.services.impl;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.models.Transfer;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.TransferService;
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

    private final NotificationClientService notificationClient;

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

    private Mono<OperationResultDto<Void>> withdrawFromSender(String login, BigDecimal amount) {
        return callAccountService(login, amount.negate());
    }

    private Mono<OperationResultDto<Void>> depositToRecipient(String login, BigDecimal amount) {
        return callAccountService(login, amount);
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

        log.error("КРИТИЧЕСКАЯ ОШИБКА: Списано у {}, но не зачислено {}", sender, recipient);
        // TODO  SAGA
        return Mono.just(createDepositErrorResponse());
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
                .message("Не удалось зачислить средства получателю")
                .build();
    }

    private Mono<OperationResultDto<Void>> callAccountService(
            String login,
            BigDecimal amount) {
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
                .then(sendTransferNotification(sender, dto))
                .thenReturn(OperationResultDto.<TransferDto>builder()
                        .success(true)
                        .data(dto)
                        .message("Перевод успешно выполнен")
                        .build());
    }

    private Mono<Void> sendTransferNotification(String login, TransferDto dto) {
        NotificationEvent event = getNotificationEvent(login, dto);

        return notificationClient.send(event)
                .doOnError(e -> log.error("Не удалось отправить уведомление для {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private NotificationEvent getNotificationEvent(String login, TransferDto dto) {
        String message = String.format("Перевод от %s пользователю %s наличных на сумму %.2f руб. завершено успешно.",
                login, dto.getLogin(), dto.getValue());

        log.info("Cash Service: Отправка подтверждения операции для {}", login);

        return NotificationEvent.builder()
                .username(login)
                .eventType(EventType.TRANSFER)
                .status(EventStatus.SUCCESS)
                .message(message)
                .sourceService("transfer-service")
                .payload(Map.of(
                        "sender_username", login,
                        "target_username", dto.getLogin(),
                        "amount", dto.getValue(),
                        "currency", "RUB"
                ))
                .build();
    }
}
