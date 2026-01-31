package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.PasswordUpdateDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserFrontService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFrontServiceImpl implements UserFrontService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry registry;
    private static final String BASE_URL = "http://gateway/api";

    @Override
    public Mono<RedirectView> updateProfile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String name = formData.getFirst("name");
                    String birthdate = formData.getFirst("birthdate");

                    if (name == null || birthdate == null) return Mono.just(errorRedirect("Данные не заполнены"));

                    return webClient.patch()
                            .uri(BASE_URL + "/account/update")
                            .bodyValue(new UserUpdateDto(name, LocalDate.parse(birthdate)))
                            .retrieve()
                            .toBodilessEntity()
                            .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("gateway-cb")))
                            .thenReturn(infoRedirect("Профиль обновлен"))
                            .onErrorResume(e -> Mono.just(errorRedirect("Ошибка обновления")));
                });
    }

    @Override
    public Mono<RedirectView> updatePassword(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String password = formData.getFirst("password");
                    String confirm = formData.getFirst("confirmPassword");

                    if (password == null || !password.equals(confirm)) {
                        return Mono.just(errorRedirect("Пароли не совпадают"));
                    }

                    return webClient.post()
                            .uri(BASE_URL + "/account/password")
                            .bodyValue(new PasswordUpdateDto(password, confirm))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                            })
                            .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("gateway-cb")))
                            .map(res -> res.isSuccess()
                                    ? infoRedirect("Пароль изменен")
                                    : errorRedirect(res.getMessage()))
                            .onErrorResume(e -> Mono.just(errorRedirect("Сервис безопасности недоступен")));
                });
    }

    @Override
    public Mono<RedirectView> openNewAccount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String currency = formData.getFirst("currency");
                    log.debug("Отправка запроса на открытие счета. Валюта: {}", currency);

                    return webClient.post()
                            .uri(uriBuilder -> getUri(uriBuilder, currency))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {})
                            .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("gateway-cb")))
                            .map(res -> res.isSuccess()
                                    ? infoRedirect("Счет в " + currency + " открыт")
                                    : errorRedirect(res.getMessage()))
                            .onErrorResume(e -> {
                                log.error("Ошибка при вызове микросервиса account: {}", e.getMessage());
                                return Mono.just(errorRedirect("Ошибка при открытии счета"));
                            });
                });
    }

    private @NotNull URI getUri(UriBuilder uriBuilder, String currency) {
        return uriBuilder
                .scheme("http")
                .host("gateway")
                .path("/api/account/open-account")
                .queryParam("currency", currency)
                .build();
    }

    private RedirectView infoRedirect(String msg) {
        return new RedirectView("/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }

    private RedirectView errorRedirect(String msg) {
        return new RedirectView("/main?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }
}
