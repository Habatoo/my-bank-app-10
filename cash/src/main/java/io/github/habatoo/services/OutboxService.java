package io.github.habatoo.services;

/**
 *
 */
public interface OutboxService {

    /**
     * Запуск каждые 5 секунд.
     * fixedDelay гарантирует, что следующая итерация начнется через 5 сек после завершения предыдущей.
     */
    void processOutboxEvents();

    /**
     * Очистка старых записей.
     * Запускается раз в час (3600000 мс).
     */
    void cleanupOldRecords();
}
