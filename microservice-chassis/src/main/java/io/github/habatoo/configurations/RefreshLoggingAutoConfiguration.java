package io.github.habatoo.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Автоконфигурация для мониторинга обновления свойств.
 * Логирует факт успешного обновления конфигурации через Actuator.
 */
@Slf4j
@AutoConfiguration
public class RefreshLoggingAutoConfiguration {

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        log.info("=== КОНФИГУРАЦИЯ ОБНОВЛЕНА ===");
        log.info("Событие (Name): {}", event.getName());
        log.info("Источник события: {}", event.getSource());
    }
}
