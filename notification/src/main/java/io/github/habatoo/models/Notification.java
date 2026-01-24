package io.github.habatoo.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая запись в истории уведомлений.
 * <p>
 * Данный класс отображается на таблицу {@code notification_history} в базе данных.
 * Хранит информацию об отправленных пользователям сообщениях для последующего
 * аудита и отображения в личном кабинете.
 */
@Table("notification_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * Уникальный идентификатор записи (Primary Key).
     * Используется тип {@link UUID} для обеспечения уникальности в распределенной системе.
     */
    @Id
    private UUID id;

    /**
     * Логин пользователя (из JWT/SecurityContext), которому предназначено уведомление.
     * Служит связующим звеном с микросервисом аккаунтов.
     */
    private String username;

    /**
     * Текст уведомления.
     * Содержит конечное сообщение, сформированное на основе события и шаблона.
     */
    private String message;

    /**
     * Дата и время отправки записи.
     * Хранится в формате {@link LocalDateTime} для корректной обработки часовых поясов
     * при взаимодействии между сервисами и БД.
     */
    @Column("sent_at")
    private LocalDateTime sentAt;
}
