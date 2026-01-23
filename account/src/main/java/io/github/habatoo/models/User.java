package io.github.habatoo.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая пользователя системы.
 * <p>
 * Данный класс отображается на таблицу {@code users} в базе данных.
 * Хранит персональные данные пользователя и информацию, необходимую для его
 * идентификации в системе через сопоставление с данными из JWT-токенов.
 * </p>
 */
@Table("users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Уникальный идентификатор пользователя (Primary Key).
     * Генерируется на стороне приложения или базы данных.
     */
    @Id
    private UUID id;

    /**
     * Логин пользователя.
     * Служит ключевым полем для интеграции с внешними системами аутентификации
     * (например, Keycloak), где он соответствует полю "preferred_username".
     */
    private String login;

    /**
     * Полное имя или ФИО пользователя.
     * Используется для корректного отображения в интерфейсе и при формировании выписок.
     */
    private String name;

    /**
     * Дата рождения пользователя.
     * Соответствует колонке {@code birth_date} и используется для бизнес-логики,
     * связанной с возрастными ограничениями.
     */
    @Column("birth_date")
    private LocalDate birthDate;

    /**
     * Дата и время регистрации пользователя в системе.
     * Заполняется автоматически при первом входе или создании профиля.
     */
    @Column("created_at")
    private OffsetDateTime createdAt;

    /**
     * Дата и время последнего изменения данных профиля пользователя.
     */
    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
