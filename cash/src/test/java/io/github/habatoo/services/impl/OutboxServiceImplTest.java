package io.github.habatoo.services.impl;

import io.github.habatoo.services.OutboxClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тестирование реализации планировщика Outbox {@link OutboxServiceImpl}.
 * <p>
 * Класс проверяет, что методы, помеченные аннотациями планировщика,
 * корректно транслируют вызовы в нижележащий клиентский сервис {@link OutboxClientService}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса OutboxServiceImpl")
class OutboxServiceImplTest {

    @Mock
    private OutboxClientService outboxClientService;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    /**
     * Тест проверяет, что метод обработки событий вызывает соответствующий метод клиента.
     * Это гарантирует, что при срабатывании планировщика логика будет запущена.
     */
    @Test
    @DisplayName("Метод processOutboxEvents: успешное делегирование вызова")
    void processOutboxEventsShouldDelegateToClientTest() {
        outboxService.processOutboxEvents();

        verify(outboxClientService, times(1)).processOutboxEvents();
    }

    /**
     * Тест проверяет, что метод очистки записей вызывает соответствующий метод клиента.
     * Важно для подтверждения автоматизации обслуживания базы данных.
     */
    @Test
    @DisplayName("Метод cleanupOldRecords: успешное делегирование вызова")
    void cleanupOldRecordsShouldDelegateToClientTest() {
        outboxService.cleanupOldRecords();

        verify(outboxClientService, times(1)).cleanupOldRecords();
    }
}
