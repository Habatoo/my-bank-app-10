package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Клиент для отправки notifications через модульуведомлений.
 * Обеспечиват единообразную отправвку уведомлений всеми сервисами через сервис уведомлений.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClientService {

    @Value("${spring.application.notification.url:unknown-service:http://notification/api/v1/notification}")
    private String notificationUrl;

    private final WebClient webClient;

    /**
     * Метод для вызова отправки в сервис уведомлений.
     *
     * @param event единое событие уведомления для отправки.
     * @return асинхронный объект результата уведомлений.
     */
    public Mono<Void> send(NotificationEvent event) {
        return webClient
                .post()
                .uri(notificationUrl)
                .bodyValue(event)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Ошибка при отправке уведомления: {}", response.statusCode());
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Уведомление успешно отправлено"));
    }
}
