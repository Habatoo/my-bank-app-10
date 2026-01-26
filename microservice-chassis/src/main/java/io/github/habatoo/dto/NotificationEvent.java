package io.github.habatoo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Универсальный DTO для Notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {

    /**
     * Кто совершил действие (из SecurityContext/JWT)
     */
    private String username;

    /**
     * Тип события
     */
    private EventType eventType;

    /**
     * Статус (SUCCESS, FAILURE)
     */
    private EventStatus status;

    /**
     * Краткое описание для лога
     */
    private String message;

    /**
     * Время события
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Гибкие данные (сумма, счета, ошибки валидации)
     */
    private Map<String, Object> payload;

    /**
     * Имя сервиса-отправителя
     */
    private String sourceService;
}
