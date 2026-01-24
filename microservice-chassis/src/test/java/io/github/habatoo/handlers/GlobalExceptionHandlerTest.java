package io.github.habatoo.handlers;

import io.github.habatoo.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit-тесты для GlobalExceptionHandler — проверяют работу перехвата исключений
 * и установку статуса.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * Проверяет обработку WebExchangeBindException:
     * - устанавливается статус 400
     */
    @Test
    @DisplayName("Обработка ошибки валидации (400 BAD_REQUEST)")
    void shouldHandleValidationExceptionTest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
        bindingResult.addError(
                new FieldError(
                        "user",
                        "email",
                        "Invalid email format")
        );

        WebExchangeBindException exception = new WebExchangeBindException(
                null,
                bindingResult
        );

        Mono<ErrorResponse> resultMono = handler.handleValidationException(exception);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(response.getMessage()).isEqualTo("Ошибка валидации данных");
                    assertThat(response.getDetails()).isNotNull();
                    assertThat(response.getDetails().get(0).getField()).isEqualTo("email");
                    assertThat(response.getDetails().get(0).getError()).isEqualTo("Invalid email format");
                })
                .verifyComplete();
    }

    /**
     * Тест обработки AccessDeniedException (ошибка 403).
     * Проверяет, что при нехватке прав доступа возвращается корректный код ошибки.
     */
    @Test
    @DisplayName("Обработка отказа доступа (403 FORBIDDEN)")
    void shouldHandleAccessDeniedExceptionTest() {
        String errorMessage = "Access is denied";
        org.springframework.security.access.AccessDeniedException exception =
                new org.springframework.security.access.AccessDeniedException(errorMessage);

        Mono<ErrorResponse> resultMono = handler.handleAccessDenied(exception);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("ACCESS_DENIED");
                    assertThat(response.getMessage()).isEqualTo(
                            "У вас недостаточно прав для выполнения данной операции");
                    assertThat(response.getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }

    /**
     * Тест обработки NoHandlerFoundException (ошибка 404).
     * - устанавливается статус 404.
     */
    @Test
    @DisplayName("Обработка отсутствия элемента (404 NOT_FOUND)")
    void shouldHandleNotFoundExceptionTest() {
        String errorMessage = "User not found with id 42";
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

        Mono<ErrorResponse> resultMono = handler.handleNotFound(exception);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("NOT_FOUND");
                    assertThat(response.getMessage()).isEqualTo(errorMessage);
                    assertThat(response.getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }

    /**
     * Тест обработки DataAccessException (ошибка работы с БД).
     * - устанавливается статус 500.
     */
    @Test
    @DisplayName("Обработка ошибок доступа к БД (500 INTERNAL_SERVER_ERROR)")
    void shouldHandleDatabaseErrorTest() {
        DataAccessException exception = new DataAccessResourceFailureException("Connection refused");

        Mono<ErrorResponse> resultMono = handler.handleDatabaseError(exception);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("DATABASE_ERROR");
                    assertThat(response.getMessage()).isEqualTo("Произошла ошибка при работе с хранилищем данных");
                })
                .verifyComplete();
    }

    /**
     * Тест глобального перехвата неизвестных ошибок (Exception).
     * - устанавливается статус 500.
     */
    @Test
    @DisplayName("Обработка общего исключения (500 INTERNAL_SERVER_ERROR)")
    void shouldHandleGenericExceptionTest() {
        Exception exception = new RuntimeException("Unexpected system failure");

        Mono<ErrorResponse> resultMono = handler.handleGenericException(exception);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
                    assertThat(response.getMessage()).isEqualTo("Внутренняя ошибка сервера");
                })
                .verifyComplete();
    }
}
