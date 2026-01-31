package io.github.habatoo.services.impl;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики обновления профиля пользователя (UserFrontServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Юнит-тесты сервиса пользователей (UserFrontServiceImpl)")
@SuppressWarnings("unchecked")
class UserFrontServiceImplTest {

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
    private UserFrontServiceImpl userService;

    @BeforeEach
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        lenient().when(circuitBreakerRegistry.circuitBreaker("gateway-cb")).thenReturn(cb);
        lenient().when(webClient.patch()).thenReturn(requestBodyUriSpec);
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        ReflectionTestUtils.setField(userService, "gatewayHost", "http://localhost:8080");
    }

    @Test
    @DisplayName("Успешное обновление профиля — Проверка редиректа")
    void shouldUpdateProfileSuccessfullyTest() {
        String formData = "name=Alex&birthdate=1995-05-20";
        MockServerWebExchange exchange = createExchange(formData);

        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        StepVerifier.create(userService.updateProfile(exchange))
                .assertNext(view -> {
                    assertThat(view.getUrl())
                            .contains("info=" + URLEncoder.encode("Профиль обновлен", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Ошибка валидации — Данные не заполнены")
    void shouldReturnErrorWhenFieldsMissingTest() {
        MockServerWebExchange exchange = createExchange("name=Alex");

        StepVerifier.create(userService.updateProfile(exchange))
                .assertNext(view -> {
                    assertThat(view.getUrl())
                            .contains("error=" + URLEncoder.encode("Данные не заполнены", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Смена пароля — Пароли не совпадают")
    void shouldReturnErrorWhenPasswordsMismatchTest() {
        MockServerWebExchange exchange = createExchange("password=123&confirmPassword=456");

        StepVerifier.create(userService.updatePassword(exchange))
                .assertNext(view -> {
                    assertThat(view.getUrl())
                            .contains("error=" + URLEncoder.encode("Пароли не совпадают", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Открытие нового счета — Успех")
    void shouldOpenAccountSuccessfullyTest() {
        MockServerWebExchange exchange = createExchange("currency=USD");

        OperationResultDto<Void> successRes = OperationResultDto.<Void>builder()
                .success(true)
                .build();

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(successRes));

        StepVerifier.create(userService.openNewAccount(exchange))
                .assertNext(view -> {
                    assertThat(view.getUrl())
                            .contains("info=" + URLEncoder.encode("Счет в USD открыт", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Открытие нового счета — Системная ошибка")
    void shouldHandleOpenAccountErrorTest() {
        MockServerWebExchange exchange = createExchange("currency=EUR");

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("API Down")));

        StepVerifier.create(userService.openNewAccount(exchange))
                .assertNext(view -> {
                    assertThat(view.getUrl())
                            .contains("error=" + URLEncoder
                                    .encode("Ошибка при открытии счета", StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    private MockServerWebExchange createExchange(String query) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .post("/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(query));
    }
}
