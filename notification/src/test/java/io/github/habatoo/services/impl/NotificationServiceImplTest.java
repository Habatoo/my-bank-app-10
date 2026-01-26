package io.github.habatoo.services.impl;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.models.Notification;
import io.github.habatoo.repositories.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тест сервиса отправки уведомлений NotificationService.
 */
@DisplayName("Проверка сервиса NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    /**
     * Параметризованный тест для проверки всех типов событий.
     * Проверяет, что для каждого EventType вызывается сохранение в БД и корректно
     * отрабатывает switch-case в processSideEffects.
     */
    @ParameterizedTest
    @EnumSource(EventType.class)
    @DisplayName("Проверка обработки всех типов событий из Enum")
    void processEventAllEventTypesSuccessTest(EventType type) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType(type)
                .username("test_user")
                .message("Test Message")
                .payload(Map.of(
                        "initialBalance", "100",
                        "amount", "50",
                        "target_username", "receiver"
                ))
                .build();

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .username("test_user")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(Mono.just(savedNotification));

        Mono<Void> result = notificationService.processEvent(event);

        StepVerifier.create(result)
                .verifyComplete();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    /**
     * Тест обработки ошибки БД.
     * Проверяет, что если репозиторий упал, side-effects не будут выполнены,
     * и ошибка пробросится дальше.
     */
    @Test
    @DisplayName("Ошибка БД: цепочка прерывается")
    void processEventDatabaseErrorFailureTest() {
        NotificationEvent event = NotificationEvent.builder()
                .eventType(EventType.REGISTRATION)
                .username("user")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(Mono.error(new RuntimeException("DB Connection Failed")));

        Mono<Void> result = notificationService.processEvent(event);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
