package io.github.habatoo.controllers;

import io.github.habatoo.AccountApplication;
import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для {@link UserController}.
 * Проверяют цепочку безопасности и корректность маппинга ответов.
 */
@SpringBootTest(
        classes = AccountApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.liquibase.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration,org.springdoc.core.configuration.SpringDocConfiguration",
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureWebTestClient
@DisplayName("Интеграционное тестирование UserController")
class UserControllerIntegrationTest {

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

    @Test
    @DisplayName("GET /user - Успешное получение профиля")
    void getCurrentUserSuccessTest() {
        String mockUsername = "active_user";
        AccountFullResponseDto expectedResponse = AccountFullResponseDto.builder()
                .login(mockUsername)
                .name("Ivan Ivanov")
                .balance(new BigDecimal("1500.00"))
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        when(userService.getOrCreateUser(any(Jwt.class)))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", mockUsername)))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountFullResponseDto.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PATCH /update - Успешное обновление профиля")
    void updateMeSuccessTest() {
        String mockUsername = "update_user";
        String name = "New Full Name";
        UserUpdateDto updateDto = new UserUpdateDto(name, LocalDate.of(1995, 10, 20));

        AccountFullResponseDto updatedResponse = AccountFullResponseDto.builder()
                .login(mockUsername)
                .name(name)
                .build();

        when(userService.updateProfile(eq(mockUsername), any(UserUpdateDto.class)))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", mockUsername)))
                .patch()
                .uri("/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo(mockUsername)
                .jsonPath("$.name").isEqualTo(name);
    }

    @Test
    @DisplayName("GET /user - Отказ в доступе без нужной роли")
    void getCurrentUserForbiddenTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus().isForbidden();
    }
}
