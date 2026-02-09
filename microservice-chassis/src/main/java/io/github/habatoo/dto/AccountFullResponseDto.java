package io.github.habatoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.habatoo.dto.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountFullResponseDto {
    private String login;
    private String name;
    private LocalDate birthDate;

    private UUID accountId;
    private BigDecimal balance;
    private Currency currency;

    private Long version;
}
