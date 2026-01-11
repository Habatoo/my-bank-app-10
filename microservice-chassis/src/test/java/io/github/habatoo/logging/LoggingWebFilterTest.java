package io.github.habatoo.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для LoggingWebFilter — проверяют работу логгера
 * и вызов цепочки фильтров.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для LoggingWebFilter")
class LoggingWebFilterTest {

    private LoggingWebFilter loggingWebFilter;

    @Mock
    private WebFilterChain filterChain;

    @BeforeEach
    void setUp() {
        loggingWebFilter = new LoggingWebFilter();
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    /**
     * Проверяем что обычный API запрос логируется и вызывать цепочку фильтров,
     * что запрос прошел дальше по цепочке.
     */
    @Test
    @DisplayName("Должен логировать обычный API запрос и вызывать цепочку фильтров")
    void shouldLogApiCallAndContinueChainTest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/account").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Mono<Void> result = loggingWebFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    /**
     * Проверяем что логика фильтрации по пути работает,
     * что метод завершился успешно.
     */
    @Test
    @DisplayName("Должен игнорировать (не логировать время) эндпоинты actuator/health")
    void shouldSkipLoggingForHealthCheckTest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );

        Mono<Void> result = loggingWebFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    /**
     * Проверяем что логика фильтрации по пути работает,
     * что метод завершился успешно при ошибке в последующих фильтрах.
     */
    @Test
    @DisplayName("Должен корректно работать при ошибке в последующих фильтрах")
    void shouldHandleErrorsInChainTest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/error").build()
        );

        when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("System Error")));

        Mono<Void> result = loggingWebFilter.filter(exchange, filterChain);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
