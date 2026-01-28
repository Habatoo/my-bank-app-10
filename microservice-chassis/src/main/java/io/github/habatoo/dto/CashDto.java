package io.github.habatoo.dto;

import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CashDto {
    private UUID id;
    private UUID userId;
    private BigDecimal value;
    private Currency currency;
    private UUID accountId;
    private OperationType action;
    private Long version;
    private LocalDateTime createdAt;
}
