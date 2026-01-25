package io.github.habatoo.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая запись о денежном переводе между пользователями.
 * Хранится в таблице "transfers" и содержит аудит-информацию об отправителе,
 * получателе и сумме транзакции.
 */
@Table("transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    /**
     * Уникальный идентификатор транзакции
     */
    @Id
    private UUID id;

    /**
     * Имя пользователя (логин) отправителя средств
     */
    @Column("sender_username")
    private String senderUsername;

    /**
     * Имя пользователя (логин) получателя средств
     */
    @Column("target_username")
    private String targetUsername;

    /**
     * Сумма перевода
     */
    private BigDecimal amount;

    /**
     * Дата и время совершения операции
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
