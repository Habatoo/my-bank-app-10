package io.github.habatoo.configurations;

import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Автоконфигурация сервисного слоя шасси микросервисов.
 * <p>
 * Данный класс отвечает за регистрацию базовых инфраструктурных сервисов,
 * необходимых для работы системы уведомлений и реализации паттерна Outbox.
 * </p>
 * <p>
 * Конфигурация включает в себя:
 * <ul>
 * <li>Клиент для взаимодействия с сервисом уведомлений.</li>
 * <li>Сервис для управления транзакционным выходящим ящиком (Outbox).</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
public class ServicesChassisAutoConfiguration {

    /**
     * Создает бин сервиса для отправки уведомлений.
     * <p>
     * Сервис использует фоновый {@link WebClient} для неблокирующих вызовов
     * и интегрирован с {@link CircuitBreakerRegistry} для обеспечения
     * отказоустойчивости при взаимодействии с внешним модулем уведомлений.
     * </p>
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
     * <p>
     * Данный сервис координирует сохранение событий в локальную базу данных
     * и их последующую передачу через {@link NotificationClientService},
     * гарантируя доставку сообщений (at-least-once delivery).
     * </p>
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
