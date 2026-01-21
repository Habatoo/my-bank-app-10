package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.services.CashService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static io.github.habatoo.dto.enums.OperationType.PUT;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry registry;

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
                .onErrorResume(e -> {
                    log.error("Системная ошибка: {}", e.getMessage());
                    return Mono.just(
                            "redirect:/main?error=" + URLEncoder.encode(
                                    "Сервис временно недоступен", StandardCharsets.UTF_8));
                });
    }

    private String getRedirect(CashDto cashDto, OperationResultDto<CashDto> result) {
        if (result.isSuccess()) {
            String actionName = (cashDto.getAction() == PUT) ? "Пополнение" : "Снятие";
            String msg = String.format("%s на сумму %s  ₽ выполнено успешно", actionName, cashDto.getValue());
            return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        } else {
            return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
        }
    }

    private URI getUri(CashDto cashDto, UriBuilder uriBuilder) {
        return uriBuilder
                .scheme("http")
                .host("gateway")
                .path("/api/main/cash")
                .queryParam("value", cashDto.getValue())
                .queryParam("action", cashDto.getAction())
                .build();
    }
}
