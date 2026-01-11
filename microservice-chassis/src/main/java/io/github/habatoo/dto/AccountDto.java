package io.github.habatoo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AccountDto {

    private UUID id;

    @NotBlank(message = "Имя пользователя (name) не может быть пустым")
    @Size(min = 2, max = 200)
    @JsonProperty("name")
    private String name;

    @NotNull(message = "Дата рождения обязательна")
    @Past(message = "Дата рождения должна быть в прошлом")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    @PositiveOrZero(message = "Сумма не может быть отрицательной")
    @JsonProperty("sum")
    private BigDecimal balance;
}
