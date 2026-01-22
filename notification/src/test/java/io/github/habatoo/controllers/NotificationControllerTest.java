package io.github.habatoo.controllers;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для {@link NotificationController} с использованием моков.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = NotificationEvent.builder()
                .username("test_user")
                .message("Hello world")
                .build();
    }

    @Test
    @DisplayName("Успех: сервис вернул результат")
    void handleNotificationSuccessWithDataTest() {
        when(notificationService.processEvent(any())).thenReturn(Mono.empty());

        Mono<OperationResultDto<Void>> result = notificationController.handleNotification(testEvent);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.isSuccess() &&
                                "Уведомление принято в обработку".equals(response.getMessage()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Ошибка: сервис выбросил исключение")
    void handleNotificationErrorTest() {
        String errorMsg = "Service failure";
        when(notificationService.processEvent(any())).thenReturn(Mono.error(new RuntimeException(errorMsg)));

        Mono<OperationResultDto<Void>> result = notificationController.handleNotification(testEvent);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        !response.isSuccess() &&
                                response.getMessage().contains(errorMsg))
                .verifyComplete();
    }
}
