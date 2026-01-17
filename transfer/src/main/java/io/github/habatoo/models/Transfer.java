package io.github.habatoo.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    private UUID id;

    @Column("sender_username")
    private String senderUsername;

    @Column("target_username")
    private String targetUsername;

    private BigDecimal amount;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
