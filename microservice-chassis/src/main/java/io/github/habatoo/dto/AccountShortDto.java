package io.github.habatoo.dto;

import io.github.habatoo.dto.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Объект передачи данных (DTO) для краткой информации об аккаунте.
 * <p>
 * Используется преимущественно для отображения списка доступных получателей
 * в форме выбора при создании перевода. Содержит минимально необходимый
 * набор полей для идентификации пользователя в интерфейсе.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class AccountShortDto {

    /**
     * Уникальный логин пользователя в системе.
     * Служит техническим идентификатором для выполнения операций перевода.
     */
    private String login;

    /**
     * Отображаемое имя пользователя (например, Имя и Фамилия).
     * Предназначено для удобного поиска и выбора получателя человеком.
     */
    private String name;

    /**
     * Валюта счета.
     */
    private Currency currency;
}
