package io.github.habatoo.configurations;

import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Автоконфигурация сервисного слоя шасси микросервисов,
 * отвечает за регистрацию базовых инфраструктурных сервисов,
 * необходимых для работы системы уведомлений и реализации паттерна Outbox.
 */
@AutoConfiguration
public class ServicesChassisAutoConfiguration {

    /**
     * Создает бин сервиса для отправки уведомлений.
     *
     * @param backgroundWebClient    специализированный клиент для фоновых задач.
     * @param circuitBreakerRegistry реестр для управления механизмами прерывания цепи.
     * @return настроенный экземпляр {@link NotificationClientService}.
     */
    @Bean
    public NotificationClientService notificationClient(
            WebClient backgroundWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        return new NotificationClientService(backgroundWebClient, circuitBreakerRegistry);
    }

    /**
     * Создает бин сервиса управления Outbox-событиями.
     *
     * @param outboxRepository   репозиторий для хранения записей Outbox.
     * @param notificationClient клиент для отправки накопленных уведомлений.
     * @return настроенный экземпляр {@link OutboxClientService}.
     */
    @Bean
    public OutboxClientService outboxService(
            OutboxRepository outboxRepository,
            NotificationClientService notificationClient) {
        return new OutboxClientService(outboxRepository, notificationClient);
    }
}
