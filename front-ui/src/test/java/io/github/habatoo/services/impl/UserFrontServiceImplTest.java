package io.github.habatoo.services.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики обновления профиля пользователя (UserFrontServiceImpl).
 * <p>
 * Проверяют извлечение данных из контекста обмена (ServerWebExchange),
 * валидацию обязательных полей и корректность PATCH-запроса к API.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Юнит-тесты сервиса пользователей (UserFrontServiceImpl)")
class UserFrontServiceImplTest {

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

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private UserFrontServiceImpl userService;

    /**
     * Базовая настройка моков WebClient для PATCH запросов.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        when(circuitBreakerRegistry.circuitBreaker("gateway-cb")).thenReturn(cb);

        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    /**
     * Тест проверяет успешное обновление профиля при заполнении всех полей.
     */
    /**
     * Тест проверяет успешное обновление профиля при заполнении всех полей.
     */
    @Test
    @DisplayName("Успешное обновление профиля — Проверка редиректа")
    void shouldUpdateProfileSuccessfullyTest() {
        String name = "Алексей";
        String birthdate = "1995-05-20";
        String formDataString = String.format("name=%s&birthdate=%s",
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(birthdate, StandardCharsets.UTF_8));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formDataString);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        Mono<RedirectView> result = userService.updateProfile(exchange);

        StepVerifier.create(result)
                .assertNext(view -> {
                    assertTrue(view.getUrl().contains("info="));
                    assertTrue(view.getUrl().contains("%D0%9F%D1%80%D0%BE%D1%84%D0%B8%D0%BB%D1%8C"));
                })
                .verifyComplete();
    }

    /**
     * Тест проверяет поведение при отсутствии обязательных полей (например, даты рождения).
     */
    @Test
    @DisplayName("Ошибка валидации — Отсутствие обязательных полей")
    void shouldReturnErrorWhenFieldsMissing() {
        String name = "Алексей";
        String formDataString = String.format("name=%s",
                URLEncoder.encode(name, StandardCharsets.UTF_8));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formDataString);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<RedirectView> result = userService.updateProfile(exchange);

        StepVerifier.create(result)
                .assertNext(view -> {
                    assertTrue(view.getUrl().contains("error=MissingFields"));
                })
                .verifyComplete();
    }

    /**
     * Тест проверяет обработку ошибки при сбое PATCH-запроса (например, ошибка 500 от API).
     */
    @Test
    @DisplayName("Системная ошибка API — Редирект с UpdateFailed")
    void shouldHandleApiError() {
        String name = "Алексей";
        String birthdate = "1995-05-20";
        String formDataString = String.format("name=%s&birthdate=%s",
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(birthdate, StandardCharsets.UTF_8));

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formDataString);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new RuntimeException("API Down")));

        Mono<RedirectView> result = userService.updateProfile(exchange);

        StepVerifier.create(result)
                .assertNext(view -> {
                    assertEquals("/main?error=UpdateFailed", view.getUrl());
                })
                .verifyComplete();
    }
}
