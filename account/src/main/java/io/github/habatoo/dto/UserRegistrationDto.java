package io.github.habatoo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class UserRegistrationDto {
    private String login;
    private String name;
    private LocalDate birthDate;
    private BigDecimal initialSum;
}
