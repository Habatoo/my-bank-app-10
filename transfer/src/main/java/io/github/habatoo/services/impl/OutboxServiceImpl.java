package io.github.habatoo.services.impl;

import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxClientService outboxClientService;

    /**
     * {@inheritDoc}
     */
    @Scheduled(fixedDelayString = "${spring.application.fixed_write_delay:5000}")
    @Override
    public void processOutboxEvents() {
        outboxClientService.processOutboxEvents();
    }

    /**
     * {@inheritDoc}
     */
    @Scheduled(fixedDelayString = "${spring.application.fixed_clean_delay:50000}")
    @Override
    public void cleanupOldRecords() {
        outboxClientService.cleanupOldRecords();
    }
}
