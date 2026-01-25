package io.github.habatoo.services;

import io.github.habatoo.BaseCashTest;
import io.github.habatoo.services.impl.OutboxServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.*;

/**
 * Интеграционный тест для {@link OutboxServiceImpl}.
 * Проверяет корректность делегирования вызовов клиенту Outbox.
 */
@TestPropertySource(properties = {
        "spring.application.fixed_write_delay=500",
        "spring.application.fixed_clean_delay=500"
})
@DisplayName("Интеграционное тестирование OutboxService")
class OutboxServiceIntegrationTest extends BaseCashTest {

    @Test
    @DisplayName("processOutboxEvents: Должен вызывать обработку событий в клиенте")
    void processOutboxEventsShouldInvokeClientMethodTest() {
        clearInvocations(outboxClientService);
        outboxService.processOutboxEvents();

        verify(outboxClientService, times(1)).processOutboxEvents();
    }

    @Test
    @DisplayName("cleanupOldRecords: Должен вызывать очистку старых записей в клиенте")
    void cleanupOldRecordsShouldInvokeClientMethodTest() {
        outboxService.cleanupOldRecords();

        verify(outboxClientService, times(1)).cleanupOldRecords();
    }

    @Test
    @DisplayName("Scheduled: Проверка автоматического срабатывания задач по расписанию")
    void scheduledTasksShouldBeInvokedBySpringContainerTest() throws InterruptedException {
        Thread.sleep(500);

        verify(outboxClientService, atLeastOnce()).processOutboxEvents();
        verify(outboxClientService, atLeastOnce()).cleanupOldRecords();
    }
}
