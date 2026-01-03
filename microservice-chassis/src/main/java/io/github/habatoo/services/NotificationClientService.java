package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class NotificationClientService {

    @Value("${spring.application.notifications.url:unknown-service:http://notifications/api/v1/notifications}")
    private String notificationsUrl;

    private final WebClient.Builder webClientBuilder;

    public Mono<Void> send(NotificationEvent event) {
        return webClientBuilder.build()
                .post()
                .uri(notificationsUrl)
                .bodyValue(event)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .bodyToMono(Void.class);
    }
}
