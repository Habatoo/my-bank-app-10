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
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final WebClient webClient;
    private final TransfersRepository transfersRepository;
    private final OutboxClientService outboxClientService;
    private final CircuitBreakerRegistry registry;

    @Value("${spring.application.gateway.host:http://gateway}")
    private String gatewayHost;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<OperationResultDto<TransferDto>> processTransferOperation(String sender, TransferDto dto) {
        String recipient = dto.getLogin();
        boolean isSelf = sender.equals(recipient);
        String depCurr = isSelf ? dto.getToCurrency().name() : dto.getFromCurrency().name();

        return callAccountService(sender, dto.getValue().negate(), dto.getFromCurrency().name())
                .flatMap(res -> res.isSuccess()
                        ? executeDepositStep(sender, recipient, dto, depCurr, isSelf)
                        : Mono.just(errorResponse("Ошибка списания: " + res.getMessage())))
                .onErrorResume(e -> Mono.just(errorResponse("Критический сбой: " + e.getMessage())));
    }

    private Mono<OperationResultDto<TransferDto>> executeDepositStep(
            String src,
            String dst,
            TransferDto dto,
            String cur,
            boolean self) {
        return callAccountService(dst, dto.getValue(), cur)
                .flatMap(res -> res.isSuccess()
                        ? finalizeTransaction(src, dst, dto, self)
                        : runCompensation(src, dto, self));
    }

    private Mono<OperationResultDto<TransferDto>> finalizeTransaction(
            String src,
            String dst,
            TransferDto dto,
            boolean self) {
        return transfersRepository.save(mapToEntity(src, dst, dto))
                .then(sendNotify(src, dto, EventStatus.SUCCESS, self, dst))
                .thenReturn(OperationResultDto.<TransferDto>builder()
                        .success(true)
                        .data(dto)
                        .message(self ? "Конвертация завершена" : "Перевод выполнен")
                        .build());
    }

    private Mono<OperationResultDto<TransferDto>> runCompensation(
            String src,
            TransferDto dto,
            boolean self) {
        return callAccountService(src, dto.getValue(), dto.getFromCurrency().name())
                .then(sendNotify(src, dto, EventStatus.FAILURE, self))
                .thenReturn(errorResponse("Ошибка зачисления. Средства возвращены."));
    }

    private Mono<OperationResultDto<Void>> callAccountService(String login, BigDecimal amt, String cur) {
        return webClient.post()
                .uri(u -> getUri(login, amt, cur, u))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                })
                .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("transfer-service-cb")));
    }

    private @NotNull URI getUri(String login, BigDecimal amt, String cur, UriBuilder u) {
        URI baseUri = URI.create(gatewayHost);
        return u.scheme(baseUri.getScheme())
                .host(baseUri.getHost())
                .port(baseUri.getPort())
                .path("/api/account/balance")
                .queryParam("login", login)
                .queryParam("amount", amt)
                .queryParam("currency", cur).build();
    }

    private Mono<Void> sendNotify(
            String user,
            TransferDto dto,
            EventStatus stat,
            boolean self,
            String... dst) {
        String msg = (stat == EventStatus.SUCCESS)
                ? (self ? String.format("Обмен %.2f %s на %s", dto.getValue(), dto.getFromCurrency(), dto.getToCurrency())
                : String.format("Перевод %s: %.2f %s", dst[0], dto.getValue(), dto.getFromCurrency()))
                : "Ошибка транзакции. Средства возвращены.";

        return outboxClientService.saveEvent(NotificationEvent.builder()
                .username(user).status(stat).message(msg).eventType(EventType.TRANSFER)
                .payload(Map.of("amt", dto.getValue(), "cur", dto.getFromCurrency())).build());
    }

    private Transfer mapToEntity(String src, String dst, TransferDto dto) {
        return Transfer.builder().senderUsername(src).targetUsername(dst)
                .amount(dto.getValue()).currency(dto.getFromCurrency()).createdAt(LocalDateTime.now()).build();
    }

    private OperationResultDto<TransferDto> errorResponse(String msg) {
        return OperationResultDto.<TransferDto>builder().success(false).message(msg).build();
    }
}
