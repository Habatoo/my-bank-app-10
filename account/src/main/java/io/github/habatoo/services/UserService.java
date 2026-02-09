package io.github.habatoo.services;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.PasswordUpdateDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Сервис для управления профилями пользователей.
 * <p>
 * Обеспечивает операции по регистрации, поиску и обновлению персональных данных.
 * Тесно интегрирован с механизмом безопасности для извлечения информации из токенов доступа.
 * </p>
 */
public interface UserService {

    /**
     * Получает информацию о пользователе или создает новую запись, если пользователь зашел впервые.
     * <p>
     * Метод реализует логику "ленивой регистрации": данные извлекаются из предоставленного JWT
     * (логин, имя, email), и на их основе формируется профиль в локальной базе данных.
     * </p>
     *
     * @param jwt объект декодированного JWT-токена с данными авторизации.
     * @return {@link Mono}, содержащий расширенную информацию о профиле {@link UserProfileResponseDto}.
     */
    Mono<UserProfileResponseDto> getOrCreateUser(Jwt jwt);

    /**
     * Обновляет персональные данные профиля пользователя.
     * <p>
     * Позволяет изменить атрибуты пользователя (например, ФИО или дату рождения)
     * на основании переданного объекта обновления.
     * </p>
     *
     * @param login уникальный логин пользователя, чей профиль необходимо обновить.
     * @param dto   объект {@link UserUpdateDto} с новыми значениями полей профиля.
     * @return {@link Mono} с актуальным состоянием профиля после обновления.
     */
    Mono<AccountFullResponseDto> updateProfile(String login, UserUpdateDto dto);


    /**
     * Обновляет пароль пользователя.
     *
     * @param login уникальный логин пользователя, чей профиль необходимо обновить.
     * @param dto   объект {@link PasswordUpdateDto} с новыми значением пароля.
     * @return {@link Mono} с актуальным состоянием пароля после обновления.
     */
    Mono<Boolean> updatePassword(String login, PasswordUpdateDto dto);
}
