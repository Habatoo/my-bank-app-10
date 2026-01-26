package io.github.habatoo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountDto {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
