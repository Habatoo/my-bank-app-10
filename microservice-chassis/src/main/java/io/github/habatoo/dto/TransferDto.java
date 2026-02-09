package io.github.habatoo.dto;

import io.github.habatoo.dto.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDto {
    private String login;
    private BigDecimal value;
    private Currency fromCurrency;
    private Currency toCurrency;
}
