package io.github.habatoo.configurations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Конфигурация Consul для микросервисов.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "spring.cloud.consul.enabled", matchIfMissing = true)
public class ConsulChassisAutoConfiguration {
}
