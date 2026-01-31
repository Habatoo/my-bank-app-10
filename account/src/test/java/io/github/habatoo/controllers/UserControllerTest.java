package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для контроллера пользователей {@link AccountController}.
 * <p>
 * Класс проверяет API для получения пользователя и изменения данных пользователя,
 * используя {@link WebTestClient} в режиме мокирования контроллера.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты контроллера UserController")
class UserControllerTest {

    private final String TEST_USERNAME = "test_user";
    @Mock
    private UserService userService;
    @InjectMocks
    private UserController userController;
    private WebTestClient webTestClient;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Mockito.mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("preferred_username")).thenReturn(TEST_USERNAME);
    }

    /**
     * Тест проверяет успешное получение профиля текущего пользователя.
     * Симулирует сценарий, при котором сервис возвращает существующего или нового пользователя.
     */
    @Test
    @DisplayName("Получение профиля: успех")
    void getCurrentUserShouldReturnProfileTest() {
        UserProfileResponseDto expectedResponse = new UserProfileResponseDto(
                "user1",
                "User One",
                LocalDate.now(),
                List.of());

        when(userService.getOrCreateUser(jwt)).thenReturn(Mono.just(expectedResponse));

        Mono<UserProfileResponseDto> result = userController.getCurrentUser(jwt);

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(userService, times(1)).getOrCreateUser(jwt);
    }

    /**
     * Тест проверяет успешное обновление данных профиля текущего пользователя.
     */
    @Test
    @DisplayName("Обновление профиля: успех")
    void updateMeShouldReturnUpdatedProfileTest() {
        UserUpdateDto updateDto = UserUpdateDto.builder()
                .name("Ivan Updated")
                .build();

        AccountFullResponseDto updatedResponse = AccountFullResponseDto.builder()
                .login(TEST_USERNAME)
                .name("Ivan Updated")
                .build();

        when(userService.updateProfile(eq(TEST_USERNAME), any(UserUpdateDto.class)))
                .thenReturn(Mono.just(updatedResponse));

        Mono<AccountFullResponseDto> result = userController.updateMe(jwt, updateDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.getName().equals("Ivan Updated")
                        && res.getLogin().equals(TEST_USERNAME))
                .verifyComplete();

        verify(userService).updateProfile(eq(TEST_USERNAME), eq(updateDto));
    }

    /**
     * Тест проверяет поведение при возникновении ошибки в сервисном слое (например, пользователь не найден).
     */
    @Test
    @DisplayName("Обновление профиля: обработка ошибки сервиса")
    void updateMeShouldHandleErrorTest() {
        UserUpdateDto updateDto = new UserUpdateDto();

        when(userService.updateProfile(anyString(), any(UserUpdateDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Ошибка обновления")));

        Mono<AccountFullResponseDto> result = userController.updateMe(jwt, updateDto);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(userService).updateProfile(eq(TEST_USERNAME), any(UserUpdateDto.class));
    }
}