package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
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

import static io.github.habatoo.dto.enums.OperationType.GET;
import static io.github.habatoo.dto.enums.OperationType.PUT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики работы сервиса операций с наличными (CashServiceImpl).
 * <p>
 * Проверяют корректность формирования запросов к API через WebClient,
 * обработку успешных ответов и сценарии возникновения ошибок.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса наличных (CashServiceImpl)")
class CashServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private CashServiceImpl cashService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        when(circuitBreakerRegistry.circuitBreaker("gateway-cb")).thenReturn(cb);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    /**
     * Тест проверяет успешное выполнение операции пополнения счета.
     * Ожидается корректный редирект с информационным сообщением.
     */
    @Test
    @DisplayName("Успешное пополнение счета — Проверка редиректа")
    void shouldReturnSuccessRedirectOnPutTest() {
        CashDto dto = CashDto.builder()
                .value(new BigDecimal("1000"))
                .action(PUT)
                .build();

        OperationResultDto<CashDto> resultDto = OperationResultDto.<CashDto>builder()
                .success(true)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(resultDto));

        Mono<String> result = cashService.moveMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertTrue(url.contains("redirect:/main?info="));
                    assertTrue(url.contains(URLEncoder.encode("Пополнение на сумму 1000", StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }

    /**
     * Тест проверяет поведение системы при получении ошибки от API.
     * Ожидается редирект с текстом ошибки, переданным от сервера.
     */
    @Test
    @DisplayName("Обработка ошибки от API — Проверка передачи сообщения")
    void shouldReturnErrorRedirectOnApiFailureTest() {
        CashDto dto = CashDto.builder()
                .value(new BigDecimal("100"))
                .action(GET)
                .build();

        String errorMessage = "Недостаточно средств";
        OperationResultDto<CashDto> resultDto = OperationResultDto.<CashDto>builder()
                .success(false)
                .message(errorMessage)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(resultDto));

        Mono<String> result = cashService.moveMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertTrue(url.contains("error=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }

    /**
     * Тест проверяет устойчивость сервиса к системным исключениям (например, недоступность Gateway).
     */
    @Test
    @DisplayName("Системная ошибка (Exception) — Проверка защитного механизма")
    void shouldHandleSystemExceptionTest() {
        CashDto dto = CashDto.builder()
                .value(new BigDecimal("100"))
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Gateway Timeout")));

        Mono<String> result = cashService.moveMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertTrue(url.contains("error=" + URLEncoder.encode("Сервис временно недоступен", StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }
}
