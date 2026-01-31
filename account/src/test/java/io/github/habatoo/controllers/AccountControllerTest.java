package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.AccountService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для контроллера аккаунтов {@link AccountController}.
 * <p>
 * Класс проверяет API для получения списка пользователей и изменения баланса,
 * используя {@link WebTestClient} в режиме мокирования контроллера.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты контроллера AccountController")
class AccountControllerTest {

    private final String TEST_USERNAME = "test_user";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private WebTestClient webTestClient;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Mockito.mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("preferred_username")).thenReturn(TEST_USERNAME);
    }

    /**
     * Тест проверяет успешное получение списка других аккаунтов.
     */
    @Test
    @DisplayName("Получение списка: успех при наличии других пользователей")
    void getListShouldReturnFluxOfAccountsTest() {
        AccountShortDto account1 = new AccountShortDto("user1", "Ivan Ivanov", Currency.RUB);
        AccountShortDto account2 = new AccountShortDto("user2", "Petr Petrov", Currency.RUB);

        when(accountService.getOtherAccounts(TEST_USERNAME)).thenReturn(Flux.just(account1, account2));

        Flux<AccountShortDto> result = accountController.getList(jwt);

        StepVerifier.create(result)
                .expectNext(account1)
                .expectNext(account2)
                .verifyComplete();

        verify(accountService).getOtherAccounts(TEST_USERNAME);
    }

    /**
     * Тест проверяет успешное изменение баланса через внутренний метод.
     */
    @Test
    @DisplayName("Обновление баланса: успешное изменение суммы")
    void updateBalanceInternalShouldReturnSuccessTest() {
        String targetLogin = "target_user";
        BigDecimal amount = new BigDecimal("1000.00");
        String currency = "RUB";

        OperationResultDto<Void> successResult = OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();

        when(accountService.changeBalance(eq(targetLogin), eq(amount), eq(currency))).thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<Void>> result = accountController.updateBalanceInternal(targetLogin, amount, currency);

        StepVerifier.create(result)
                .expectNextMatches(OperationResultDto::isSuccess)
                .verifyComplete();

        verify(accountService).changeBalance(targetLogin, amount, currency);
    }

    /**
     * Тест проверяет поведение системы, если сервис возвращает ошибку (например, пользователь не найден).
     */
    @Test
    @DisplayName("Обновление баланса: ошибка, если пользователь не найден")
    void updateBalanceInternalShouldReturnErrorWhenUserNotFoundTest() {
        String invalidLogin = "non_existent";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "RUB";

        OperationResultDto<Void> errorResult = OperationResultDto.<Void>builder()
                .success(false)
                .message("Пользователь не найден")
                .build();

        when(accountService.changeBalance(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Mono.just(errorResult));

        Mono<OperationResultDto<Void>> result = accountController.updateBalanceInternal(invalidLogin, amount, currency);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess() && res.getMessage().equals("Пользователь не найден"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Открытие счета: успешный сценарий")
    void openAccountSuccessTest() {
        String login = "user1";
        String currency = "USD";
        OperationResultDto<Void> successResult = OperationResultDto.<Void>builder()
                .success(true)
                .message("Счет открыт")
                .build();

        when(accountService.openAccount(login, currency))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<Void>> result = accountController.openAccount(jwt, currency);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess() && "Счет открыт" .equals(res.getMessage()))
                .verifyComplete();

        verify(accountService).openAccount(login, currency);
    }
}
