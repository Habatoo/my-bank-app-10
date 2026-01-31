package io.github.habatoo.models;

import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая запись о кассовой операции в базе данных.
 * <p>
 * Используется для хранения истории всех транзакций с наличными средствами
 * (депозиты и снятия), привязанных к конкретному пользователю.
 * Класс отображается на таблицу {@code operations} в схеме данных.
 * </p>
 */
@Table("operations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "id")
public class Cash {

    /**
     * Уникальный идентификатор записи об операции.
     * Генерируется автоматически на стороне приложения или БД.
     */
    @Id
    private UUID id;

    /**
     * Логин (username) пользователя, совершившего операцию.
     * Является связующим звеном с сущностью пользователя в системе.
     */
    @Column("username")
    private String username;

    /**
     * Сумма денежных средств, участвующая в операции.
     * Всегда хранится в виде абсолютного положительного значения.
     */
    private BigDecimal amount;

    /**
     * Валюта операции.
     * Используется {@link Currency} для обозначения вида валюты.
     */
    private Currency currency;

    /**
     * Тип финансовой операции.
     * Определяет характер движения денежных средств (например, DEPOSIT или WITHDRAW).
     */
    @Column("operation_type")
    private OperationType operationType;

    /**
     * Метка времени создания записи.
     * Сохраняет точное время проведения операции с учетом смещения часового пояса (UTC).
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
