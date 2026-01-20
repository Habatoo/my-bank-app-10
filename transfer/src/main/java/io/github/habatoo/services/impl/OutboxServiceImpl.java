package io.github.habatoo.services.impl;

import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxClientService outboxClientService;

    /**
     * Запуск каждые 5 секунд.
     * fixedDelay гарантирует, что следующая итерация начнется через 5 сек после завершения предыдущей.
     */
    @Scheduled(fixedDelayString = "${spring.application.fixed_write_delay:5000}")
    @Override
    public void processOutboxEvents() {
        outboxClientService.processOutboxEvents();
    }

    /**
     * Очистка старых записей.
     * Запускается раз в 50 секунд.
     */
    @Scheduled(fixedDelayString = "${spring.application.fixed_clean_delay:50000}")
    @Override
    public void cleanupOldRecords() {
        outboxClientService.cleanupOldRecords();
    }
}
