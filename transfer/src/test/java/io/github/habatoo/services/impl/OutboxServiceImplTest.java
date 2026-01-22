package io.github.habatoo.services.impl;

import io.github.habatoo.services.OutboxClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Модульные тесты для реализации сервиса Outbox {@link OutboxServiceImpl}.
 * Проверяет корректность делегирования вызовов сервису клиентской логики.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование сервиса управления Outbox (OutboxServiceImpl)")
class OutboxServiceImplTest {

    @Mock
    private OutboxClientService outboxClientService;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    /**
     * Тест проверяет, что вызов метода обработки событий
     * корректно транслируется в соответствующий клиентский сервис.
     */
    @Test
    @DisplayName("Метод processOutboxEvents должен вызывать обработку в OutboxClientService")
    void shouldCallProcessOutboxEventsOnClientService() {
        outboxService.processOutboxEvents();

        verify(outboxClientService, times(1)).processOutboxEvents();
        verifyNoMoreInteractions(outboxClientService);
    }

    /**
     * Тест проверяет, что вызов метода очистки записей
     * корректно транслируется в соответствующий клиентский сервис.
     */
    @Test
    @DisplayName("Метод cleanupOldRecords должен вызывать очистку в OutboxClientService")
    void shouldCallCleanupOldRecordsOnClientService() {
        outboxService.cleanupOldRecords();

        verify(outboxClientService, times(1)).cleanupOldRecords();
        verifyNoMoreInteractions(outboxClientService);
    }

    /**
     * Тест проверяет поведение системы при возникновении исключения в клиентском сервисе.
     * Гарантирует, что OutboxServiceImpl пробрасывает исключение выше.
     */
    @Test
    @DisplayName("Метод должен корректно пробрасывать исключения при ошибках в клиентском сервисе")
    void shouldThrowExceptionWhenClientServiceFails() {
        doThrow(new RuntimeException("Ошибка БД")).when(outboxClientService).processOutboxEvents();

        assertThrows(RuntimeException.class, () -> outboxService.processOutboxEvents());
        verify(outboxClientService, times(1)).processOutboxEvents();
    }
}
