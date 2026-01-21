package io.github.habatoo.configurations;

import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class ServicesChassisAutoConfiguration {

    @Bean
    public NotificationClientService notificationClient(
            WebClient backgroundWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new NotificationClientService(backgroundWebClient, circuitBreakerRegistry);
    }

    @Bean
    public OutboxClientService outboxService(
            OutboxRepository outboxRepository,
            NotificationClientService notificationClient) {
        return new OutboxClientService(outboxRepository, notificationClient);
    }
}
