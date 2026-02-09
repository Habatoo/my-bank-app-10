package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для {@link AccountController}.
 * Проверяют цепочку безопасности, работу аннотаций @PreAuthorize и корректность обработки JWT.
 */
@WebFluxTest(controllers = AccountController.class)
@Import(SecurityChassisAutoConfiguration.class)
@DisplayName("Интеграционное тестирование AccountController")
class AccountControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    /**
     * Тест проверяет доступ к списку пользователей для роли USER.
     * Проверяется извлечение клейма "preferred_username".
     */
    @Test
    @DisplayName("GET /users - Успех для роли USER")
    void getListSuccessTest() {
        String mockUsername = "test_user";
        AccountShortDto account = new AccountShortDto(mockUsername, "Test Account", Currency.RUB);

        when(accountService.getOtherAccounts(mockUsername)).thenReturn(Flux.just(account));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", mockUsername)))
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AccountShortDto.class)
                .hasSize(1);
    }

    /**
     * Тест проверяет срабатывание защиты @PreAuthorize.
     * Пользователь с ролью GUEST не должен иметь доступа к методу.
     */
    @Test
    @DisplayName("GET /users - Отказ доступа для некорректной роли")
    void getListForbiddenTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .get()
                .uri("/users")
                .exchange()
                .expectStatus().isForbidden();
    }

    /**
     * Тест проверяет внутренний метод изменения баланса.
     * Требуется роль ADMIN или ACCOUNT_ACCESS.
     */
    @Test
    @DisplayName("POST /balance - Успех для роли ADMIN")
    void updateBalanceSuccessTest() {
        OperationResultDto<Void> successResult = OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();
        when(accountService.changeBalance(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Mono.just(successResult));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", "client1")
                        .queryParam("amount", "100.00")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    /**
     * Тест проверяет, что без токена метод возвращает 401 Unauthorized.
     */
    @Test
    @DisplayName("POST /balance - Отказ без авторизации")
    void updateBalanceUnauthorizedTest() {
        webTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", "any")
                        .queryParam("amount", "10")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
