package io.github.habatoo.controllers;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Реактивный контроллер для управления уведомлениями.
 * <p>
 * Данный контроллер является точкой входа для всех микросервисов системы,
 * желающих отправить уведомление пользователю или зафиксировать системное событие.
 * Обработка запросов происходит в неблокирующем стиле с использованием Project Reactor.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Обрабатывает входящее событие уведомления.
     * <p>
     * Метод принимает объект {@link NotificationEvent}, содержащий информацию о типе события,
     * пользователе и дополнительные данные (payload). Событие передается в бизнес-логику
     * для дальнейшей маршрутизации (отправка Email, Push или сохранение в историю).
     *
     * @param event объект события уведомления, передаваемый в теле запроса (JSON).
     * @return {@link Mono}, содержащий {@link OperationResultDto} с результатом операции.
     * В случае успеха возвращает статус "Уведомление принято в обработку".
     * В случае ошибки возвращает DTO с флагом успеха false и текстом исключения.
     * @see NotificationEvent
     * @see NotificationService#processEvent(NotificationEvent)
     */
    @PostMapping("/notification")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_ACCESS')")
    public Mono<OperationResultDto<Void>> handleNotification(@RequestBody NotificationEvent event) {
        log.debug("Получено событие для уведомления: {}", event);

        return notificationService.processEvent(event)
                .thenReturn(OperationResultDto.<Void>builder()
                        .success(true)
                        .message("Уведомление принято в обработку")
                        .build())
                .onErrorResume(e -> {
                    log.error("Ошибка при обработке уведомления для пользователя {}: {}",
                            event.getUsername(), e.getMessage());
                    return Mono.just(OperationResultDto.<Void>builder()
                            .success(false)
                            .message("Ошибка при обработке уведомления: " + e.getMessage())
                            .build());
                });
    }
}
