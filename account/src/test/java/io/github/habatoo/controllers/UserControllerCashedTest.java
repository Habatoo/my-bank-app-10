package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционное тестирование эндпоинтов UserController.
 * Проверяет корректность маппинга JSON, извлечение данных из JWT и права доступа.
 */
@WebFluxTest(controllers = UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        SecurityChassisAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование эндпоинтов UserController")
class UserControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    /**
     * Тест проверяет получение данных профиля текущего пользователя.
     */
    @Test
    @DisplayName("GET /user - Успешное получение данных профиля")
    void getCurrentUserSuccess() {
        String username = "test_user";
        AccountFullResponseDto responseDto = AccountFullResponseDto.builder()
                .login(username)
                .name("Иван Иванов")
                .balance(new BigDecimal("1500.00"))
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        when(userService.getOrCreateUser(any())).thenReturn(Mono.just(responseDto));

        webTestClient
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", username))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.login").isEqualTo(username)
                .jsonPath("$.name").isEqualTo("Иван Иванов")
                .jsonPath("$.balance").isEqualTo(1500.00);
    }

    /**
     * Тест проверяет частичное обновление профиля пользователя.
     */
    @Test
    @DisplayName("PATCH /update - Успешное обновление данных профиля")
    void updateMeSuccess() {
        String username = "test_user";
        UserUpdateDto updateDto = new UserUpdateDto("Новое Имя", LocalDate.of(1995, 10, 20));

        AccountFullResponseDto updatedResponse = AccountFullResponseDto.builder()
                .login(username)
                .name("Новое Имя")
                .build();

        when(userService.updateProfile(eq(username), any(UserUpdateDto.class)))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", username))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .patch()
                .uri("/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Новое Имя")
                .jsonPath("$.login").isEqualTo(username);
    }

    /**
     * Проверка безопасности: доступ запрещен без необходимой роли.
     */
    @Test
    @DisplayName("GET /user - Ошибка 403 (Forbidden) при неверной роли")
    void getCurrentUserForbidden() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isForbidden();
    }

    /**
     * Проверка безопасности: обновление профиля требует авторизации.
     */
    @Test
    @DisplayName("PATCH /update - Ошибка 401 (Unauthorized) без токена")
    void updateMeUnauthorized() {
        webTestClient
                .patch()
                .uri("/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserUpdateDto("Name", LocalDate.now()))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
