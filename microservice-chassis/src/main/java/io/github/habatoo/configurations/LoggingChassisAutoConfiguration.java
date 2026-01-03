package io.github.habatoo.configurations;

import io.github.habatoo.logging.LoggingWebFilter;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Конфигурация логирования для микросервисов.
 */
@AutoConfiguration
public class LoggingChassisAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Bean
    public ObservationRegistry observationRegistry() {
        ObservationRegistry registry = ObservationRegistry.create();

        registry.observationConfig().observationFilter(context ->
                context.addLowCardinalityKeyValue(KeyValue.of("app.name", applicationName))
        );

        return registry;
    }

    @Bean
    public LoggingWebFilter loggingWebFilter() {
        return new LoggingWebFilter();
    }
}
