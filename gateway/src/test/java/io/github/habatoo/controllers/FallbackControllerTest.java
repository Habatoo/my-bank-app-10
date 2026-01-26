package io.github.habatoo.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тестирование логики {@link FallbackController} без загрузки контекста Spring.
 * <p>
 * Данный класс проверяет чистое поведение методов контроллера, гарантируя, что
 * возвращаемые реактивные объекты {@link reactor.core.publisher.Mono} содержат
 * корректные HTTP-статусы и текстовые уведомления для пользователя.
 * </p>
 */
@DisplayName("Тестирование контроллера резервных ответов (FallbackController)")
class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    /**
     * Тестирование резервного ответа при недоступности сервиса кассовых операций.
     * <p>
     * Проверяет, что при вызове метода возвращается статус 503 и специфичное
     * сообщение об ошибке для Cash Service.
     * </p>
     */
    @Test
    @DisplayName("Проверка fallback-ответа для Cash Service")
    void cashFallbackTest() {
        StepVerifier.create(fallbackController.cashFallback())
                .assertNext(response -> {
                    assertResponse(response, "Сервис кассовых операций временно недоступен. Попробуйте позже.");
                })
                .verifyComplete();
    }

    /**
     * Тестирование резервного ответа при недоступности сервиса управления счетами.
     * <p>
     * Проверяет корректность текстового уведомления, информирующего пользователя
     * о проблемах в Account Service.
     * </p>
     */
    @Test
    @DisplayName("Проверка fallback-ответа для Account Service")
    void accountFallbackTest() {
        StepVerifier.create(fallbackController.accountFallback())
                .assertNext(response -> {
                    assertResponse(response, "Сервис по работе со счетом временно недоступен. Попробуйте позже.");
                })
                .verifyComplete();
    }

    /**
     * Тестирование резервного ответа при недоступности сервиса межбанковских переводов.
     * <p>
     * Гарантирует, что пользователь получит актуальную информацию о невозможности
     * совершения перевода в данный момент.
     * </p>
     */
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
     * Универсальный метод для верификации структуры ответа сервера.
     * <p>
     * Проверяет соответствие HTTP статуса значению {@link HttpStatus#SERVICE_UNAVAILABLE} (503)
     * и эквивалентность тела ответа ожидаемой строке.
     * </p>
     *
     * @param response     полученный объект {@link ResponseEntity}
     * @param expectedBody ожидаемый текст сообщения
     */
    private void assertResponse(ResponseEntity<String> response, String expectedBody) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(expectedBody);
    }
}
