package io.github.habatoo.handlers;

import io.github.habatoo.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Глобальный перехватчик исключений — для централизованной обработки ошибок в контроллерах.
 * Возвращает страницы ошибок с сообщением и кодом.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 — Ошибка валидации (@Valid / @Validated)
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidationException(WebExchangeBindException e) {
        List<ErrorResponse.ValidationError> details = e.getFieldErrors().stream()
                .map(err -> new ErrorResponse.ValidationError(err.getField(), err.getDefaultMessage()))
                .toList();

        log.warn("Validation failed for {}: {}", e.getObjectName(), details);

        return Mono.just(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Ошибка валидации данных")
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    /**
     * 404 — Ресурс не найден
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleNotFound(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse("NOT_FOUND", e.getMessage());
    }

    /**
     * 500 — Ошибка БД
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleDatabaseError(DataAccessException e) {
        log.error("Database error: {}", e.getMessage(), e);

        return buildResponse("DATABASE_ERROR", "Произошла ошибка при работе с хранилищем данных");
    }

    /**
     * 500 — Все остальные исключения
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: ", e);

        return buildResponse("INTERNAL_SERVER_ERROR", "Внутренняя ошибка сервера");
    }

    private Mono<ErrorResponse> buildResponse(String code, String message) {
        return Mono.just(ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
