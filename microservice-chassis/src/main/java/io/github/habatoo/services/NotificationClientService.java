package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
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

    @Value("${spring.application.notification.url:http://localhost:8085/notification}")
    private String notificationUrl;
    private final WebClient backgroundWebClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Метод для вызова отправки в сервис уведомлений.
     *
     * @param event единое событие уведомления для отправки.
     * @return асинхронный объект результата уведомлений.
     */
    public Mono<Void> sendScheduled(NotificationEvent event) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("notification-service-cb");

        return backgroundWebClient
                .post()
                .uri(notificationUrl)
                .bodyValue(event)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Ошибка при отправке уведомления. Статус: {}, Тело: {}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Notification ошибка сервиса"));
                                })
                )
                .toBodilessEntity()
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorResume(e -> Mono.empty())
                .doOnSuccess(v -> log.debug("Уведомление успешно доставлено в модуль уведомлений"))
                .then();
    }
}
