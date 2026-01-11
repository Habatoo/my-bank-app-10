package io.github.habatoo.services;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Блок данных аккаунта пользователя
 * Состоит из:
 * поля фамилии и имени (с возможностью редактирования);
 * даты рождения (с возможностью редактирования);
 * текущей суммы на счёте (без возможности редактирования);
 * кнопки «Сохранить изменения».
 * Предусмотрена валидация: все поля заполнены, возраст старше 18 лет. При сохранении невалидных данных появляется ошибка.
 */
public interface UserService {

    /**
     *
     * @param userName
     * @return
     */
    Mono<Void> updateUserName(String userName);

    /**
     *
     * @param birthDate
     * @return
     */
    Mono<Void> updateBirthdate(LocalDate birthDate);

}
