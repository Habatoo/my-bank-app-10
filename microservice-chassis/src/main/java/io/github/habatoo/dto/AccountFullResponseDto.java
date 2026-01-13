package io.github.habatoo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AccountFullResponseDto {
    private String login;
    private String name;
    private LocalDate birthDate;

    private UUID accountId;
    private BigDecimal balance;

    private Long version;
}
