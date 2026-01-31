package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.PasswordUpdateDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Контроллер для управления данными профиля текущего пользователя.
 * <p>
 * Предоставляет эндпоинты для получения расширенной информации о пользователе
 * и частичного обновления данных профиля. Все операции выполняются в контексте
 * текущего авторизованного сеанса.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Получение полной информации о текущем пользователе.
     *
     * @param jwt объект авторизованного пользователя, содержащий данные токена.
     * @return {@link Mono}, содержащий полную информацию о профиле {@link AccountFullResponseDto}.
     */
    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ACCOUNT_ACCESS')")
    public Mono<UserProfileResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Запрос данных профиля для пользователя: {}", jwt.getClaimAsString("preferred_username"));
        return userService.getOrCreateUser(jwt);
    }

    /**
     * Обновление данных профиля текущего пользователя.
     *
     * @param jwt объект авторизованного пользователя для идентификации.
     * @param dto объект с обновленными данными пользователя.
     * @return {@link Mono} с обновленной информацией о профиле.
     */
    @PatchMapping("/update")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ACCOUNT_ACCESS')")
    public Mono<AccountFullResponseDto> updateMe(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestBody UserUpdateDto dto) {
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Запрос на обновление профиля пользователем: {}", username);
        return userService.updateProfile(username, dto);
    }

    /**
     * Обновление пароля текущего пользователя.
     *
     * @param jwt объект авторизованного пользователя для идентификации.
     * @param dto объект с обновленными данными пользователя.
     * @return {@link Mono} с результатом обновления.
     */
    @PostMapping("/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ACCOUNT_ACCESS')")
    public Mono<Boolean> updatePassword(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody PasswordUpdateDto dto) {
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Запрос на обновление пароля пользователем: {}", username);
        return userService.updatePassword(username, dto);
    }
}
