package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import reactor.core.publisher.Mono;

/**
 * Сервис обработки и маршрутизации уведомлений.
 * <p>
 * Является центральным звеном бизнес-логики модуля уведомлений.
 * Отвечает за интерпретацию входящих событий, выбор способа доставки
 * (Email, Push, SMS) и сохранение истории уведомлений.
 */
public interface NotificationService {

    /**
     * Обрабатывает входящее событие в реактивном режиме.
     * <p>
     * Метод выполняет следующие действия:
     * <ul>
     * <li>Анализирует тип события {@link io.github.habatoo.dto.enums.EventType};</li>
     * <li>Формирует текстовое сообщение на основе данных из {@code event.getPayload()};</li>
     * <li>Инициирует отправку через доступные каналы связи;</li>
     * <li>Сохраняет запись о событии в базу данных через {@link io.github.habatoo.repositories.NotificationRepository}.</li>
     * </ul>
     *
     * @param event объект события, содержащий метаданные и контекст уведомления.
     * @return {@link Mono<Void>}, который завершается успешно после того, как событие
     * было принято и поставлено в очередь на отправку/запись.
     * @see io.github.habatoo.dto.NotificationEvent
     */
    Mono<Void> processEvent(NotificationEvent event);
}
