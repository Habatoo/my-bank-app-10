package io.github.habatoo.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для {@link FallbackController}.
 * Проверяют корректность возвращаемых сообщений и HTTP-статусов при отказах сервисов.
 */
@DisplayName("Тестирование контроллера резервных ответов (FallbackController)")
class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    @Test
    @DisplayName("Проверка fallback-ответа для Cash Service")
    void cashFallbackTest() {
        StepVerifier.create(fallbackController.cashFallback())
                .assertNext(response -> {
                    assertResponse(response, "Сервис кассовых операций временно недоступен. Попробуйте позже.");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка fallback-ответа для Account Service")
    void accountFallbackTest() {
        StepVerifier.create(fallbackController.accountFallback())
                .assertNext(response -> {
                    assertResponse(response, "Сервис по работе со счетом временно недоступен. Попробуйте позже.");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка fallback-ответа для Transfer Service")
    void transferFallbackTest() {
        StepVerifier.create(fallbackController.transferFallback())
                .assertNext(response -> {
                    assertResponse(response, "Сервис переводов временно недоступен. Попробуйте позже.");
                })
                .verifyComplete();
    }

    /**
     * Вспомогательный метод для проверки структуры ResponseEntity.
     */
    private void assertResponse(ResponseEntity<String> response, String expectedBody) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(expectedBody);
    }
}
