package io.github.habatoo.controllers;

import io.github.habatoo.GatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Интеграционные тесты для контроллера FallbackController.
 * <p>
 * Проверяет поведение fallback-эндпоинтов при недоступности сервисов.
 * Используется WebTestClient для отправки запросов и проверки ответов.
 * Security полностью отключен для тестов через TestSecurityConfig.
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@ContextConfiguration(classes = FallbackControllerIntegrationTest.TestSecurityConfig.class)
class FallbackControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Проверка fallback для недоступного сервиса кассовых операций.
     * Ожидается статус 503 и соответствующее сообщение.
     */
    @Test
    void cashFallbackTest() {
        webTestClient.get()
                .uri("/fallback/cash-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .isEqualTo("Сервис кассовых операций временно недоступен. Попробуйте позже.");
    }

    /**
     * Проверка fallback для недоступного сервиса по работе со счетом.
     * Ожидается статус 503 и соответствующее сообщение.
     */
    @Test
    void accountFallbackTest() {
        webTestClient.get()
                .uri("/fallback/account-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .isEqualTo("Сервис по работе со счетом временно недоступен. Попробуйте позже.");
    }

    /**
     * Проверка fallback для недоступного сервиса переводов.
     * Ожидается статус 503 и соответствующее сообщение.
     */
    @Test
    void transferFallbackTest() {
        webTestClient.get()
                .uri("/fallback/transfer-unavailable")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .isEqualTo("Сервис переводов временно недоступен. Попробуйте позже.");
    }

    /**
     * Тестовая конфигурация Security для отключения всех ограничений.
     * Все запросы разрешены, CSRF отключен.
     *
     * @Primary гарантирует переопределение любой другой Security-конфигурации.
     */
    @Configuration
    static class TestSecurityConfig {

        @Bean
        @Primary
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(auth -> auth.anyExchange().permitAll())
                    .build();
        }
    }
}
