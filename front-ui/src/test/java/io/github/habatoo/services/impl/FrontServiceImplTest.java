package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.RateClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики работы сервиса операций с фронтом (FrontServiceImpl).
 * <p>
 * Проверяют корректность формирования запросов к API через WebClient,
 * обработку успешных ответов и сценарии возникновения ошибок.
 * </p>
 */
@SuppressWarnings("rawtypes")
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты фронт-сервиса (FrontServiceImpl)")
class FrontServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RateClientService rateClientService;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @InjectMocks
    private FrontServiceImpl frontService;

    @BeforeEach
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("accountServiceCB");
        when(circuitBreakerRegistry.circuitBreaker("accountServiceCB")).thenReturn(cb);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(rateClientService.takeRate(any(Currency.class), any(Currency.class))).thenReturn(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Успешная загрузка данных главной страницы")
    void shouldReturnRenderingWithFullData() {
        AccountDto myAccountRub = AccountDto.builder()
                .balance(new BigDecimal("1500.00"))
                .currency(Currency.RUB)
                .build();

        AccountDto myAccountUsd = AccountDto.builder()
                .balance(new BigDecimal("99.00"))
                .currency(Currency.USD)
                .build();

        UserProfileResponseDto profile = UserProfileResponseDto.builder()
                .name("Ivan Ivanov")
                .login("ivan")
                .birthDate(LocalDate.of(1990, 5, 15))
                .accounts(List.of(myAccountRub, myAccountUsd))
                .build();

        AccountShortDto otherUser = AccountShortDto.builder()
                .login("petr")
                .name("Petr Petrov")
                .build();

        when(responseSpec.bodyToMono(UserProfileResponseDto.class)).thenReturn(Mono.just(profile));
        when(responseSpec.bodyToFlux(AccountShortDto.class)).thenReturn(Flux.just(otherUser));

        Mono<Rendering> result = frontService.showMainPage("Welcome", null);

        StepVerifier.create(result)
                .assertNext(rendering -> {
                    assertThat(rendering.view()).isEqualTo("main");
                    var model = rendering.modelAttributes();
                    assertThat(model.get("name")).isEqualTo("Ivan Ivanov");
                    assertThat(model.get("sum")).isEqualTo(new BigDecimal("1599.00"));
                    assertThat(model.get("info")).isEqualTo("Welcome");

                    List<?> otherUsersList = (List<?>) model.get("accounts");
                    assertThat(otherUsersList).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Частичный успех: Список других пользователей недоступен")
    void shouldHandleOtherUsersErrorButShowProfile() {
        UserProfileResponseDto profile = UserProfileResponseDto.builder()
                .name("Ivan")
                .accounts(List.of())
                .build();

        when(responseSpec.bodyToMono(UserProfileResponseDto.class)).thenReturn(Mono.just(profile));
        when(responseSpec.bodyToFlux(AccountShortDto.class)).thenReturn(Flux.error(new RuntimeException("Flux Error")));

        Mono<Rendering> result = frontService.showMainPage(null, null);

        StepVerifier.create(result)
                .assertNext(rendering -> {
                    assertThat(rendering.view()).isEqualTo("main");
                    List<?> others = (List<?>) rendering.modelAttributes().get("accounts");
                    assertThat(others).isEmpty();
                    assertThat(rendering.modelAttributes().get("name")).isEqualTo("Ivan");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Полный отказ: Ошибка профиля — Возврат страницы с ошибкой")
    void shouldHandleProfileError() {
        when(responseSpec.bodyToMono(UserProfileResponseDto.class))
                .thenReturn(Mono.error(new RuntimeException("Service Down")));
        when(responseSpec.bodyToFlux(AccountShortDto.class)).thenReturn(Flux.empty());

        Mono<Rendering> result = frontService.showMainPage(null, null);

        StepVerifier.create(result)
                .assertNext(rendering -> {
                    assertThat(rendering.view()).isEqualTo("main");
                    List<String> errors = (List<String>) rendering.modelAttributes().get("errors");
                    assertThat(errors).contains("Сервис временно недоступен");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка расчета суммы баланса")
    void shouldCalculateTotalBalanceCorrectly() {
        AccountDto acc1 = AccountDto.builder().balance(new BigDecimal("100.50")).currency(Currency.CNY).build();
        AccountDto acc2 = AccountDto.builder().balance(new BigDecimal("200.40")).currency(Currency.RUB).build();
        AccountDto acc3 = AccountDto.builder().balance(new BigDecimal("300.10")).currency(Currency.USD).build();

        UserProfileResponseDto profile = UserProfileResponseDto.builder()
                .accounts(List.of(acc1, acc2, acc3))
                .build();

        when(responseSpec.bodyToMono(UserProfileResponseDto.class)).thenReturn(Mono.just(profile));
        when(responseSpec.bodyToFlux(AccountShortDto.class)).thenReturn(Flux.empty());

        Mono<Rendering> result = frontService.showMainPage(null, null);

        StepVerifier.create(result)
                .assertNext(rendering -> {
                    BigDecimal total = (BigDecimal) rendering.modelAttributes().get("sum");
                    assertThat(total).isEqualByComparingTo("601.00");
                })
                .verifyComplete();
    }
}
