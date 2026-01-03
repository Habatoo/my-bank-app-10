package io.github.habatoo.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Класс для биндинга настроек из файла конфигурации.
 * <p>
 * Связывает свойства с префиксом "spring.resilience" из application.yml
 * Содержит параметры для настройки.
 * <p>
 */
@ConfigurationProperties(prefix = "spring.resilience")
public record ResilienceProperties(
        String instanceName,
        Long slidingWindowSize,
        Long failureRateThreshold,
        Long waitDurationInOpenState,
        Long permittedNumberOfCallsInHalfOpenState
) {
}
