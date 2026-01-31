package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики сервиса переводов (TransferFrontServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса переводов (TransferFrontServiceImpl)")
@SuppressWarnings("unchecked")
class TransferFrontServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @InjectMocks
    private TransferFrontServiceImpl transferService;

    @BeforeEach
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        lenient().when(circuitBreakerRegistry.circuitBreaker("gateway-cb")).thenReturn(cb);

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Успешный перевод — Проверка формирования URL")
    void shouldReturnSuccessRedirectTest() {
        TransferDto dto = new TransferDto();
        dto.setLogin("receiver_user");
        dto.setValue(new BigDecimal("500"));
        dto.setFromCurrency(Currency.USD);

        OperationResultDto<TransferDto> successResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(successResponse));

        Mono<String> result = transferService.sendMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertThat(url).startsWith("redirect:/main?info=");
                    String expectedMsg = "Перевод пользователю на сумму 500 USD выполнен успешно";
                    assertThat(url).contains(URLEncoder.encode(expectedMsg, StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Внутренний перевод (самому себе) — Одинаковые валюты")
    void shouldReturnInfoRedirectWhenCurrenciesSameTest() {
        TransferDto dto = new TransferDto();
        dto.setFromCurrency(Currency.RUB);
        dto.setToCurrency(Currency.RUB);

        Mono<String> result = transferService.sendMoneyToSelf(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertThat(url).startsWith("redirect:/main?info=");
                    String expectedMsg = "Счета совпадают, баланс не изменился";
                    assertThat(url).contains(URLEncoder.encode(expectedMsg, StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Внутренний перевод (самому себе) — Разные валюты (WebClient)")
    void shouldWorkWithDifferentCurrenciesInSelfTransferTest() {
        TransferDto dto = new TransferDto();
        dto.setFromCurrency(Currency.USD);
        dto.setToCurrency(Currency.RUB);
        dto.setValue(new BigDecimal("100"));

        OperationResultDto<TransferDto> successResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(successResponse));

        Mono<String> result = transferService.sendMoneyToSelf(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertThat(url).contains("info=");
                    assertThat(url).contains(URLEncoder.encode("Внутренний перевод", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Ошибка перевода — Сообщение от API")
    void shouldReturnErrorRedirectFromApiTest() {
        TransferDto dto = new TransferDto();
        dto.setLogin("unknown");
        dto.setValue(new BigDecimal("100"));

        String apiErrorMessage = "Пользователь не найден";
        OperationResultDto<TransferDto> errorResponse = OperationResultDto.<TransferDto>builder()
                .success(false)
                .message(apiErrorMessage)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(errorResponse));

        Mono<String> result = transferService.sendMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertThat(url).contains("error=" + URLEncoder.encode(apiErrorMessage, StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Системное исключение — Проверка префикса ошибки")
    void shouldHandleWebClientExceptionTest() {
        TransferDto dto = new TransferDto();
        dto.setValue(new BigDecimal("10"));

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Mono<String> result = transferService.sendMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    String expectedPart = URLEncoder.encode("Ошибка: Connection refused", StandardCharsets.UTF_8);
                    assertThat(url).contains(expectedPart);
                })
                .verifyComplete();
    }
}
