package io.github.habatoo.account.services;

import io.github.habatoo.account.BaseAccountTest;
import io.github.habatoo.services.impl.OutboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * Интеграционный тест для {@link OutboxServiceImpl}.
 * Проверяет корректность делегирования вызовов клиенту Outbox.
 */
@TestPropertySource(properties = {
        "spring.application.fixed_write_delay=500",
        "spring.application.fixed_clean_delay=500"
})
@DisplayName("Интеграционное тестирование OutboxService")
class OutboxServiceIntegrationTest extends BaseAccountTest {

    @BeforeEach
    void resetMock() {
        reset(outboxClientService);
    }

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
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    verify(outboxClientService, atLeastOnce()).processOutboxEvents();
                    verify(outboxClientService, atLeastOnce()).cleanupOldRecords();
                });
    }
}
