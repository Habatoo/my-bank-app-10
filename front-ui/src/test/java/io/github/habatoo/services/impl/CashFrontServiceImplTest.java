package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики работы сервиса операций с наличными (CashFrontServiceImpl).
 * <p>
 * Проверяют корректность формирования запросов к API через WebClient,
 * обработку успешных ответов и сценарии возникновения ошибок.
 * </p>
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса наличных (CashFrontServiceImpl)")
class CashFrontServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CircuitBreakerRegistry registry;

    @InjectMocks
    private CashFrontServiceImpl cashService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        lenient().when(registry.circuitBreaker("gateway-cb")).thenReturn(cb);

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Успешное пополнение счета — Проверка редиректа")
    void shouldReturnSuccessRedirectOnPutTest() {
        BigDecimal value = new BigDecimal("1000.00");
        CashDto dto = CashDto.builder()
                .value(value)
                .action(OperationType.PUT)
                .currency(Currency.RUB)
                .build();

        OperationResultDto<CashDto> resultDto = OperationResultDto.<CashDto>builder()
                .success(true)
                .data(dto)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(resultDto));

        Mono<String> result = cashService.moveMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertThat(url).startsWith("redirect:/main?info=");
                    assertThat(url).contains("1000");
                    assertThat(url).contains("RUB");
                    assertThat(url).contains(URLEncoder.encode("выполнено успешно", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

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

    @Test
    @DisplayName("Системная ошибка (Exception) — Проверка защитного механизма")
    void shouldHandleSystemExceptionTest() {
        CashDto dto = CashDto.builder()
                .value(new BigDecimal("100"))
                .action(OperationType.PUT)
                .currency(Currency.RUB)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Gateway Timeout")));

        Mono<String> result = cashService.moveMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    String expectedErrorMsg = URLEncoder.encode("Сервис операций временно недоступен", StandardCharsets.UTF_8);
                    assertThat(url)
                            .startsWith("redirect:/main?error=")
                            .contains(expectedErrorMsg);
                })
                .verifyComplete();
    }
}
