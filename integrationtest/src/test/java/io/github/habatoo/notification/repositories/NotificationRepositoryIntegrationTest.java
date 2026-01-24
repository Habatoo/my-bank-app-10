package io.github.habatoo.notification.repositories;

import io.github.habatoo.models.Notification;
import io.github.habatoo.notification.BaseNotificationTest;
import io.github.habatoo.repositories.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link NotificationRepository}.
 * Проверяют корректность взаимодействия с таблицей уведомлений в PostgreSQL.
 */
@DisplayName("Тестирование NotificationRepository")
public class NotificationRepositoryIntegrationTest extends BaseNotificationTest {

    @Test
    @DisplayName("Save & FindById: Успешное сохранение и получение уведомления")
    void saveAndFindByIdShouldWorkTest() {
        Notification notification = createNotification("user", "Test Message");

        var action = clearDatabase()
                .then(notificationRepository.save(notification))
                .flatMap(saved -> notificationRepository.findById(saved.getId()));

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found.getId()).isNotNull();
                    assertThat(found.getMessage()).isEqualTo("Test Message");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("FindById: Возврат пустого Mono, если уведомление не найдено")
    void findByIdShouldReturnEmptyWhenNotFoundTest() {
        var action = clearDatabase()
                .then(notificationRepository.findById(UUID.randomUUID()));

        StepVerifier.create(action)
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete: Успешное удаление уведомления")
    void deleteShouldWorkTest() {
        Notification notification = createNotification("target", "To Delete");

        var action = clearDatabase()
                .then(notificationRepository.save(notification))
                .flatMap(saved -> notificationRepository.deleteById(saved.getId()))
                .then(notificationRepository.findAll().collectList());

        StepVerifier.create(action)
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Update: Успешное обновление статуса прочтения")
    void updateIsReadStatusTest() {
        Notification notification = createNotification("user", "Update Me");


        var action = clearDatabase()
                .then(notificationRepository.save(notification))
//                .map(n -> {
//                    n.setIsRead(true);
//                    return n;
//                })
                .flatMap(notificationRepository::save)
                .flatMap(saved -> notificationRepository.findById(saved.getId()));

        StepVerifier.create(action)
                .assertNext(found -> assertThat(found.getMessage().contains("Update Me")))
                .verifyComplete();
    }
}
