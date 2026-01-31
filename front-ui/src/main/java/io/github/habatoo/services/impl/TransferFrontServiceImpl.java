package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferFrontService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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

import java.net.URI;
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

    @Value("${spring.application.gateway.host:http://gateway}")
    private String gatewayHost;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> sendMoney(TransferDto transferDto) {
        return executeTransfer(transferDto, "/api/main/transfer", "Перевод пользователю");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> sendMoneyToSelf(TransferDto transferDto) {
        if (transferDto.getFromCurrency() == transferDto.getToCurrency()) {
            return Mono.just("redirect:/main?info=" + URLEncoder.encode("Счета совпадают, баланс не изменился", StandardCharsets.UTF_8));
        }
        return executeTransfer(transferDto, "/api/main/self-transfer", "Внутренний перевод");
    }

    /**
     * Общий приватный метод для выполнения запроса через WebClient
     */
    private Mono<String> executeTransfer(
            TransferDto transferDto,
            String path,
            String successPrefix) {
        log.info("{} запрос: от {} в {} на сумму {}",
                successPrefix, transferDto.getFromCurrency(),
                transferDto.getLogin() != null ? transferDto.getLogin() : "self",
                transferDto.getValue());

        CircuitBreaker cb = registry.circuitBreaker("gateway-cb");

        return webClient.post()
                .uri(uriBuilder -> getUri(path, uriBuilder))
                .bodyValue(transferDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<TransferDto>>() {
                })
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .map(result -> getResult(transferDto, successPrefix, result))
                .onErrorResume(e -> getError(path, e));
    }

    private @NotNull Mono<String> getError(String path, Throwable e) {
        log.error("Ошибка перевода в {}: {}", path, e.getMessage());
        String errorMsg = "Ошибка: " + (e.getMessage().contains("404") ? "Сервис недоступен" : e.getMessage());
        return Mono.just("redirect:/main?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
    }

    private @NotNull String getResult(
            TransferDto transferDto,
            String successPrefix,
            OperationResultDto<TransferDto> result) {
        if (result.isSuccess()) {
            String msg = getMsg(transferDto, successPrefix);
            return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        } else {
            return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
        }
    }

    private @NotNull String getMsg(TransferDto transferDto, String successPrefix) {
        return successPrefix +
                " на сумму " +
                transferDto.getValue() +
                " " +
                transferDto.getFromCurrency() +
                " выполнен успешно";
    }

    private @NotNull URI getUri(String path, UriBuilder uriBuilder) {
        URI baseUri = URI.create(gatewayHost);

        return uriBuilder
                .scheme(baseUri.getScheme())
                .host(baseUri.getHost())
                .port(baseUri.getPort())
                .path(path)
                .build();
    }
}
