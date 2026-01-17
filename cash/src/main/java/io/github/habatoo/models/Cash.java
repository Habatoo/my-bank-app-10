package io.github.habatoo.models;

import io.github.habatoo.dto.enums.OperationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("operations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cash {

    @Id
    private UUID id;

    @Column("username")
    private String username;

    private BigDecimal amount;

    @Column("operation_type")
    private OperationType operationType;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
