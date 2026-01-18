package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики сервиса переводов (TransferServiceImpl).
 * <p>
 * Тестирование охватывает сценарии успешного перевода средств,
 * бизнес-ошибок (например, неверный логин) и системных сбоев.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса переводов (TransferServiceImpl)")
class TransferServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private TransferServiceImpl transferService;

    /**
     * Настройка цепочки вызовов WebClient перед каждым тестом.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    /**
     * Проверка формирования редиректа при успешном переводе.
     */
    @Test
    @DisplayName("Успешный перевод — Проверка формирования URL")
    void shouldReturnSuccessRedirectTest() {
        TransferDto dto = new TransferDto();
        dto.setLogin("receiver_user");
        dto.setValue(new BigDecimal("500"));

        OperationResultDto<TransferDto> successResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(successResponse));

        Mono<String> result = transferService.sendMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertTrue(url.contains("info="));
                    String expectedMsg = "Перевод пользователю + receiver_user на сумму 500 ₽ выполнено успешно";
                    assertTrue(url.contains(URLEncoder.encode(expectedMsg, StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }

    /**
     * Проверка обработки ошибки, возвращенной от API (например, пользователь не найден).
     */
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
                    assertTrue(url.contains("error=" + URLEncoder.encode(apiErrorMessage, StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }

    /**
     * Проверка обработки исключения при запросе (например, 500 ошибка сервера или обрыв связи).
     */
    @Test
    @DisplayName("Системное исключение — Редирект с текстом ошибки")
    void shouldHandleWebClientExceptionTest() {
        TransferDto dto = new TransferDto();
        dto.setLogin("user1");
        dto.setValue(new BigDecimal("10"));

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Mono<String> result = transferService.sendMoney(dto);

        StepVerifier.create(result)
                .assertNext(url -> {
                    assertTrue(url.contains("error="));
                    assertTrue(url.contains(URLEncoder.encode("Ошибка перевода: Connection refused", StandardCharsets.UTF_8)));
                })
                .verifyComplete();
    }
}
