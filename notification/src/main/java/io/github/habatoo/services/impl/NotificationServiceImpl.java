package io.github.habatoo.services.impl;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.models.Notification;
import io.github.habatoo.repositories.NotificationRepository;
import io.github.habatoo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> processEvent(NotificationEvent event) {
        Notification notification = obtainNotification(event);

        log.info("Начало обработки события {} для пользователя {}", event.getEventType(), event.getUsername());

        return notificationRepository.save(notification)
                .doOnNext(saved -> log.info("Запись успешно сохранена в БД с ID: {}", saved.getId()))
                .doOnSuccess(notification1 -> processSideEffects(event))
                .then();
    }

    private void processSideEffects(NotificationEvent event) {
        switch (event.getEventType()) {
            case REGISTRATION -> sendWelcomeEmail(event);
            case DEPOSIT, WITHDRAW -> sendPushNotification(event);
            case TRANSFER -> sendTransferNotifications(event);
            case UPDATE_PROFILE -> sendUpdateProfile(event);
            case SYSTEM_ALERT -> sendAlert(event);
            default -> sendUnknownNotification(event);
        }
    }

    private void sendWelcomeEmail(NotificationEvent event) {
        log.info("Email: Добро пожаловать, {}! На вашем счету {}",
                event.getUsername(), event.getPayload().get("initialBalance"));
    }

    private void sendPushNotification(NotificationEvent event) {
        log.info("Push: Изменение баланса на сумму {}", event.getPayload().get("amount"));
    }

    private void sendTransferNotifications(NotificationEvent event) {
        log.info("Push: Перевод пользователю {} на сумму {}",
                event.getPayload().get("target_username"), event.getPayload().get("amount"));
    }

    private void sendUpdateProfile(NotificationEvent event) {
        log.info("Профиль {} обновлен, отправка SMS не требуется", event.getUsername());
    }

    private void sendAlert(NotificationEvent event) {
        log.info("dAlert для {}: {}", event.getUsername(), event.getMessage());
    }

    private void sendUnknownNotification(NotificationEvent event) {
        log.warn("Неизвестный тип события: {}", event.getEventType());
    }

    private Notification obtainNotification(NotificationEvent event) {
        return Notification.builder()
                .username(event.getUsername())
                .message(event.getMessage())
                .sentAt(LocalDateTime.now())
                .build();
    }
}
