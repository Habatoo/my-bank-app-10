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
 * Тестирование реализации сервиса Outbox {@link OutboxServiceImpl}.
 * Проверяет корректность вызова методов обработки и очистки событий через клиентский сервис.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса OutboxServiceImpl")
class OutboxServiceImplTest {

    @Mock
    private OutboxClientService outboxClientService;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    /**
     * Тест проверки вызова метода обработки событий.
     * Проверяет, что сервис делегирует задачу по расписанию в OutboxClientService.
     */
    @Test
    @DisplayName("Обработка событий: вызов клиентского сервиса")
    void processOutboxEventsCallTest() {
        outboxService.processOutboxEvents();

        verify(outboxClientService, times(1)).processOutboxEvents();
    }

    /**
     * Тест проверки вызова метода очистки старых записей.
     * Проверяет корректное делегирование задачи очистки.
     */
    @Test
    @DisplayName("Очистка записей: вызов клиентского сервиса")
    void cleanupOldRecordsCallTest() {
        outboxService.cleanupOldRecords();

        verify(outboxClientService, times(1)).cleanupOldRecords();
    }
}
