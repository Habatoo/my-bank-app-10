package io.github.habatoo.notification.services;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.notification.BaseNotificationTest;
import io.github.habatoo.services.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для {@link NotificationService} с полным подъемом контекста Spring Boot.
 * Проверяет бизнес-логику управления счетами, включая транзакционность и работу с репозиториями.
 */
@DisplayName("Интеграционное тестирование NotificationService")
class NotificationServiceIntegrationTest extends BaseNotificationTest {

    @Test
    @DisplayName("processEvent: Успешное сохранение уведомления при регистрации")
    void processRegistrationEventShouldSaveNotificationTest() {
        NotificationEvent event = NotificationEvent.builder()
                .username("ivan_tester")
                .eventType(EventType.REGISTRATION)
                .message("Добро пожаловать в банк!")
                .payload(Map.of("initialBalance", "100.00"))
                .build();

        var action = clearDatabase()
                .then(notificationService.processEvent(event))
                .thenMany(notificationRepository.findAll());

        StepVerifier.create(action)
                .assertNext(savedNotification -> {
                    assertThat(savedNotification.getId()).isNotNull();
                    assertThat(savedNotification.getUsername()).isEqualTo("ivan_tester");
                    assertThat(savedNotification.getMessage()).isEqualTo("Добро пожаловать в банк!");
                    assertThat(savedNotification.getSentAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("processEvent: Обработка события перевода (TRANSFER)")
    void processTransferEventShouldWorkCorrectlyTest() {
        NotificationEvent event = NotificationEvent.builder()
                .username("sender_user")
                .eventType(EventType.TRANSFER)
                .message("Перевод выполнен")
                .payload(Map.of(
                        "target_username", "receiver_user",
                        "amount", "500"
                ))
                .build();

        var action = clearDatabase()
                .then(notificationService.processEvent(event))
                .then(notificationRepository.findAll().next());

        StepVerifier.create(action)
                .assertNext(notification -> {
                    assertThat(notification.getUsername()).isEqualTo("sender_user");
                    assertThat(notification.getMessage()).contains("Перевод выполнен");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("processEvent: Проверка логики для SYSTEM_ALERT")
    void processSystemAlertEventTest() {
        NotificationEvent event = NotificationEvent.builder()
                .username("admin")
                .eventType(EventType.SYSTEM_ALERT)
                .message("Технические работы")
                .payload(Map.of())
                .build();

        var action = clearDatabase()
                .then(notificationService.processEvent(event))
                .then(notificationRepository.count());

        StepVerifier.create(action)
                .expectNext(1L)
                .verifyComplete();
    }
}
