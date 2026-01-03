package io.github.habatoo.configurations;

import io.github.habatoo.services.NotificationClientService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class NotificationChassisAutoConfiguration {

    @Bean
    public NotificationClientService notificationClient(WebClient.Builder webClientBuilder) {
        return new NotificationClientService(webClientBuilder);
    }
}
