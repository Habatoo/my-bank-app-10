package io.github.habatoo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Для списка выбора в форме перевода
 */
@Data
@AllArgsConstructor
public class AccountShortDto {
    private String login;
    private String name;
}
