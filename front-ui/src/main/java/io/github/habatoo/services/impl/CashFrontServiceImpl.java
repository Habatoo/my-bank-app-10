package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashFrontService;
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
public class CashFrontServiceImpl implements CashFrontService {

    private static final String API_URL = "/api/main/cash";

    private final WebClient webClient;
    private final CircuitBreakerRegistry registry;

    @Value("${spring.application.gateway.host:http://gateway}")
    private String gatewayHost;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> moveMoney(CashDto cashDto) {
        CircuitBreaker cb = registry.circuitBreaker("gateway-cb");

        return webClient.post()
                .uri(uriBuilder -> getUri(cashDto, uriBuilder))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<CashDto>>() {
                })
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .map(result -> getRedirect(cashDto, result))
                .onErrorResume(this::getError);
    }

    private @NotNull Mono<String> getError(Throwable e) {
        log.error("Системная ошибка при работе с наличными: {}", e.getMessage());
        return Mono.just("redirect:/main?error=" +
                URLEncoder.encode("Сервис операций временно недоступен", StandardCharsets.UTF_8));
    }

    private String getRedirect(CashDto cashDto, OperationResultDto<CashDto> result) {
        if (result.isSuccess()) {
            String actionName = (cashDto.getAction() == OperationType.PUT) ? "Пополнение" : "Снятие";
            String msg = String.format("%s на сумму %.2f %s выполнено успешно",
                    actionName, cashDto.getValue(), cashDto.getCurrency());
            return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        }
        return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
    }

    private @NotNull URI getUri(CashDto cashDto, UriBuilder uriBuilder) {
        URI baseUri = URI.create(gatewayHost);

        return uriBuilder
                .scheme(baseUri.getScheme())
                .host(baseUri.getHost())
                .port(baseUri.getPort())
                .path(API_URL)
                .queryParam("value", cashDto.getValue())
                .queryParam("action", cashDto.getAction())
                .queryParam("currency", cashDto.getCurrency())
                .build();
    }
}
