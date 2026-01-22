package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для клиентского сервиса уведомлений NotificationClientService.
 * Проверяют взаимодействие с WebClient и корректную работу Circuit Breaker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса NotificationClientService")
class NotificationClientServiceTest {

    @Mock
    private WebClient backgroundWebClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private NotificationClientService notificationClientService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.ResponseSpec responseSpec;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    @SuppressWarnings("rawtype")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private final String testUrl = "http://localhost:8085/notification";
    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationClientService, "notificationUrl", testUrl);

        testEvent = NotificationEvent.builder()
                .username("test_user")
                .message("Test message")
                .build();

        when(circuitBreakerRegistry.circuitBreaker("notification-service-cb")).thenReturn(circuitBreaker);
    }

    /**
     * Тест успешной отправки уведомления.
     * Проверяет, что цепочка выполняется до конца и возвращает пустой Mono.
     */
    @Test
    @DisplayName("Успех: Отправка уведомления завершена удачно")
    void sendScheduledSuccessTest() {
        mockWebClientChain();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        Mono<Void> result = notificationClientService.sendScheduled(testEvent);

        StepVerifier.create(result)
                .verifyComplete();

        verify(backgroundWebClient).post();
    }

    /**
     * Тест обработки HTTP-ошибки.
     * Проверяет, что при возникновении ошибки срабатывает onErrorResume и возвращается Mono.empty().
     */
    @Test
    @DisplayName("Ошибка: Сервис уведомлений вернул 500, поток подавляет ошибку")
    void sendScheduledHttpErrorShouldReturnEmptyTest() {
        mockWebClientChain();
        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);
            ClientResponse mockResponse = mock(ClientResponse.class);
            when(mockResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
            when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("Server Error Body"));

            errorHandler.apply(mockResponse).subscribe();
            return responseSpec;
        });
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new RuntimeException("Notification ошибка сервиса")));

        Mono<Void> result = notificationClientService.sendScheduled(testEvent);

        StepVerifier.create(result)
                .verifyComplete();
    }

    /**
     * Тест срабатывания Circuit Breaker.
     * Проверяет поведение, когда Circuit Breaker запрещает вызов.
     */
    @Test
    @DisplayName("Circuit Breaker: Подавление ошибки при открытом состоянии")
    void sendScheduledCircuitBreakerOpenTest() {
        mockWebClientChain();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new RuntimeException("CB Open")));

        Mono<Void> result = notificationClientService.sendScheduled(testEvent);

        StepVerifier.create(result)
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(3));
    }

    /**
     * Вспомогательный метод для мокирования флоу WebClient.
     */
    private void mockWebClientChain() {
        when(backgroundWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }
}