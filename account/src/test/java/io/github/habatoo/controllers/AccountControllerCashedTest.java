package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционное тестирование эндпоинтов AccountController.
 * Проверяет безопасность (Security), маппинг параметров и корректность ответов в реактивной среде.
 */
@WebFluxTest(controllers = AccountController.class)
@ContextConfiguration(classes = {
        AccountController.class,
        SecurityChassisAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование эндпоинтов AccountController")
class AccountControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    /**
     * Проверяет получение списка сторонних аккаунтов.
     * Эмулирует пользователя с ролью ROLE_USER.
     */
    @Test
    @DisplayName("GET /users - Успех при получении списка других пользователей")
    void getListSuccess() {
        String currentUser = "current_authorized_user";
        AccountShortDto otherUser = new AccountShortDto("other_user", "Иван Иванов", Currency.RUB);

        when(accountService.getOtherAccounts(currentUser)).thenReturn(Flux.just(otherUser));

        webTestClient
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", currentUser))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].login").isEqualTo("other_user")
                .jsonPath("$[0].name").isEqualTo("Иван Иванов");
    }

    /**
     * Проверяет внутренний метод обновления баланса.
     * Требует роль ROLE_ADMIN.
     */
    @Test
    @DisplayName("POST /balance - Успешное обновление баланса администратором")
    void updateBalanceInternalSuccess() {
        String targetLogin = "target_user";
        BigDecimal amount = new BigDecimal("500.00");
        String currency = "RUB";

        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();

        when(accountService.changeBalance(eq(targetLogin), eq(amount), eq(currency)))
                .thenReturn(Mono.just(successResponse));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", targetLogin)
                        .queryParam("amount", amount)
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Баланс обновлен");
    }

    /**
     * Проверяет, что обычный пользователь не может вызвать административный метод обновления баланса.
     */
    @Test
    @DisplayName("POST /balance - Ошибка 403 (Forbidden) для обычного пользователя")
    void updateBalanceInternalForbidden() {
        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", "any")
                        .queryParam("amount", 100)
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    /**
     * Проверяет доступ к списку пользователей без авторизации.
     */
    @Test
    @DisplayName("GET /users - Ошибка 401 (Unauthorized) без токена")
    void getListUnauthorized() {
        webTestClient
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Успешное открытие счета администратором")
    @WithMockUser(roles = "ADMIN")
    void openAccountShouldReturnSuccessWhenUserIsAdminTest() {
        String login = "testuser";
        String currency = "USD";
        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder()
                .success(true)
                .message("Счет успешно открыт")
                .build();

        when(accountService.openAccount(login, currency))
                .thenReturn(Mono.just(successResponse));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/account")
                        .queryParam("login", login)
                        .queryParam("currency", currency)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Счет успешно открыт");

        verify(accountService, times(1)).openAccount(login, currency);
    }

    @Test
    @DisplayName("Ошибка открытия счета при неверных параметрах")
    @WithMockUser(roles = "ACCOUNT_ACCESS")
    void openAccountShouldReturnErrorWhenServiceFailsTest() {
        OperationResultDto<Void> errorResponse = OperationResultDto.<Void>builder()
                .success(false)
                .errorCode("INVALID_CURRENCY")
                .message("Допустимые валюты: RUB, USD, CNY")
                .build();

        when(accountService.openAccount(anyString(), anyString()))
                .thenReturn(Mono.just(errorResponse));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/account")
                        .queryParam("login", "user")
                        .queryParam("currency", "EUR")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("INVALID_CURRENCY");
    }

    @Test
    @DisplayName("Доступ запрещен для пользователя без нужной роли")
    @WithMockUser(roles = "USER")
    void openAccountShouldReturnForbiddenWhenUserHasNoRightsTest() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/account")
                        .queryParam("login", "any")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(accountService);
    }
}
