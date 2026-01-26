package io.github.habatoo.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая финансовый счет пользователя.
 * <p>
 * Данный класс отображается на таблицу {@code account} в базе данных.
 * Используется для хранения актуального баланса и отслеживания временных меток
 * создания и обновления записи.
 * </p>
 */
@Table("account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /**
     * Уникальный идентификатор счета (Primary Key).
     */
    @Id
    private UUID id;

    /**
     * Идентификатор пользователя, которому принадлежит данный счет.
     * Соответствует колонке {@code user_id} в базе данных.
     */
    @Column("user_id")
    private UUID userId;

    /**
     * Текущий баланс денежных средств на счету.
     * Используется {@link BigDecimal} для обеспечения высокой точности финансовых операций.
     */
    private BigDecimal balance;

    /**
     * Поле для реализации оптимистической блокировки (Optimistic Locking).
     * Позволяет предотвратить потерю данных при одновременном обновлении баланса
     * из разных транзакций.
     */
    @Version
    private Long version;

    /**
     * Дата и время создания счета.
     * Сохраняется с указанием часового пояса.
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления информации о счету (например, при изменении баланса).
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
