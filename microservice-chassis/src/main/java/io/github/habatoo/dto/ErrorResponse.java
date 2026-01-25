package io.github.habatoo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Единая модель ответа об ошибке
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * Код ошибки (например, APP_DATA_NOT_FOUND)
     */
    private String code;

    /**
     * Человекочитаемое описание
     */
    private String message;


    private LocalDateTime timestamp;

    /**
     * Для ошибок валидации @Valid
     */
    private List<ValidationError> details;

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String error;
    }
}
