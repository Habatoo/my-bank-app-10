package io.github.habatoo.configurations;

import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class ServicesChassisAutoConfiguration {

    @Bean
    public NotificationClientService notificationClient(
            WebClient webClient,
            WebClient backgroundWebClient) {
        return new NotificationClientService(webClient, backgroundWebClient);
    }

    @Bean
    public OutboxClientService outboxService(
            OutboxRepository outboxRepository,
            NotificationClientService notificationClient) {
        return new OutboxClientService(outboxRepository, notificationClient);
    }
}
