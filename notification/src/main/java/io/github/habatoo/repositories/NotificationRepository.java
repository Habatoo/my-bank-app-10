package io.github.habatoo.repositories;

import io.github.habatoo.models.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

/**
 * Репозиторий для работы с сущностью {@link Notification}.
 * <p>
 * Обеспечивает неблокирующий доступ к таблице {@code notification_history}
 * с использованием Spring Data R2DBC. Позволяет выполнять CRUD-операции
 * над историей уведомлений в реактивном потоке.
 *
 * @see Notification
 * @see R2dbcRepository
 */
public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {
}
