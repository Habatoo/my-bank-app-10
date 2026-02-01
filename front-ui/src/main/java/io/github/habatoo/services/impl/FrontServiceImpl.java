package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.FrontService;
import io.github.habatoo.services.RateClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrontServiceImpl implements FrontService {

    private final WebClient webClient;
    private final RateClientService rateClientService;
    private final CircuitBreakerRegistry registry;

    @Value("${spring.application.gateway.host:http://gateway}")
    private String gatewayHost;

    @Override
    public Mono<Rendering> showMainPage(String info, String error) {
        return fetchUserProfile()
                .zipWith(fetchOtherUsers())
                .map(tuple -> {
                    UserProfileResponseDto profile = tuple.getT1();
                    List<AccountShortDto> otherUsers = tuple.getT2();

                    return Rendering.view("main")
                            .modelAttribute("name", profile.getName())
                            .modelAttribute("login", profile.getLogin())
                            .modelAttribute("birthdate", profile.getBirthDate())
                            .modelAttribute("sum", calculateTotalBalance(profile.getAccounts()))
                            .modelAttribute("userAccounts", profile.getAccounts())
                            .modelAttribute("accounts", otherUsers)
                            .modelAttribute("info", info)
                            .modelAttribute("errors", error != null ? List.of(error) : null)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Ошибка загрузки данных главной страницы: {}", e.getMessage());
                    return Mono.just(Rendering.view("main")
                            .modelAttribute("errors", List.of("Сервис временно недоступен"))
                            .build());
                });
    }

    /**
     * Получает профиль текущего пользователя и все его счета.
     */
    private Mono<UserProfileResponseDto> fetchUserProfile() {
        return webClient.get()
                .uri(getApiUrl("/api/main/user"))
                .retrieve()
                .bodyToMono(UserProfileResponseDto.class)
                .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("accountServiceCB")))
                .doOnError(e -> log.error("Error fetching profile: {}", e.getMessage()));
    }

    /**
     * Получает список других пользователей системы для переводов.
     */
    private Mono<List<AccountShortDto>> fetchOtherUsers() {
        return webClient.get()
                .uri(getApiUrl("/api/main/users"))
                .retrieve()
                .bodyToFlux(AccountShortDto.class)
                .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("accountServiceCB")))
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * Суммирует балансы всех счетов пользователя, переводя каждый в RUB.
     */
    private BigDecimal calculateTotalBalance(List<AccountDto> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return accounts.stream()
                .map(acc -> calcAmount(acc.getBalance(), acc.getCurrency(), Currency.RUB))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Конвертирует сумму из одной валюты в другую через RateClientService.
     */
    private BigDecimal calcAmount(BigDecimal value, Currency fromCurrency, Currency toCurrency) {
        if (value == null) return BigDecimal.ZERO;

        try {
            BigDecimal rate = rateClientService.takeRate(fromCurrency, toCurrency);
            return rate.multiply(value);
        } catch (Exception e) {
            log.warn("Не удалось получить курс для {} -> {}: {}", fromCurrency, toCurrency, e.getMessage());
            return fromCurrency == toCurrency ? value : BigDecimal.ZERO;
        }
    }

    private String getApiUrl(String path) {
        return UriComponentsBuilder.fromUriString(gatewayHost)
                .path(path)
                .toUriString();
    }
}
