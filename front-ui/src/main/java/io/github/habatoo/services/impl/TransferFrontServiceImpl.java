package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.RateClientService;
import io.github.habatoo.services.TransferFrontService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferFrontServiceImpl implements TransferFrontService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry registry;
    private final RateClientService rateClientService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> sendMoney(TransferDto transferDto) {
        prepareRecipientData(transferDto);
        return executeTransfer(transferDto, "/api/main/transfer", "Перевод пользователю");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> sendMoneyToSelf(TransferDto transferDto) {
        if (transferDto.getFromCurrency() == transferDto.getToCurrency()) {
            return Mono.just("redirect:/main?info=" + URLEncoder.encode(
                    "Счета совпадают, баланс не изменился", StandardCharsets.UTF_8));
        }
        return executeTransfer(transferDto, "/api/main/self-transfer", "Внутренний перевод");
    }

    /**
     * Разбирает строку "login:currency" из поля login и заполняет поля DTO.
     */
    private void prepareRecipientData(TransferDto dto) {
        String rawLogin = dto.getLogin();
        if (rawLogin != null && rawLogin.contains(":")) {
            String[] parts = rawLogin.split(":");
            dto.setLogin(parts[0]);
            try {
                dto.setToCurrency(Currency.valueOf(parts[1].toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.error("Некорректная валюта в данных формы: {}", parts[1]);
                dto.setToCurrency(dto.getFromCurrency());
            }
        } else if (dto.getToCurrency() == null) {
            dto.setToCurrency(dto.getFromCurrency());
        }
    }

    /**
     * Общий приватный метод для выполнения запроса через WebClient
     */
    private Mono<String> executeTransfer(TransferDto dto, String path, String prefix) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<TransferDto>>() {
                })
                .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("gateway-cb")))
                .map(result -> getTransferResult(dto, prefix, result))
                .onErrorResume(this::getError);
    }

    private @NotNull Mono<String> getError(Throwable e) {
        log.error("Системная ошибка при выполнении перевода: {}", e.getMessage());
        return Mono.just("redirect:/main?error=" + URLEncoder.encode(
                "Сервис переводов недоступен", StandardCharsets.UTF_8));
    }

    private @NotNull String getTransferResult(
            TransferDto dto,
            String prefix,
            OperationResultDto<TransferDto> result) {
        if (result.isSuccess()) {
            BigDecimal rate = rateClientService.takeRate(dto.getFromCurrency(), dto.getToCurrency());
            BigDecimal target = dto.getValue().multiply(rate).setScale(2, RoundingMode.HALF_UP);

            String msg = String.format("%s: списано %.2f %s, зачислено %.2f %s",
                    prefix, dto.getValue(), dto.getFromCurrency(), target, dto.getToCurrency());

            return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        }
        return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
    }
}
