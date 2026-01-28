package io.github.habatoo.services;

import io.github.habatoo.BaseAccountTest;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.models.User;
import io.github.habatoo.services.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для {@link AccountServiceImpl} с полным подъемом контекста Spring Boot.
 * Проверяет бизнес-логику управления счетами, включая транзакционность и работу с репозиториями.
 */
@DisplayName("Интеграционное тестирование AccountService")
class AccountServiceIntegrationTest extends BaseAccountTest {

    @BeforeEach
    void setUp() {
        when(outboxClientService.saveEvent(ArgumentMatchers.any(NotificationEvent.class)))
                .thenReturn(Mono.empty());
    }

    /**
     * Тестирует получение полной информации об аккаунте по логину.
     */
    @Test
    @DisplayName("getByLogin: Должен возвращать полную информацию при существовании данных")
    void getByLoginShouldReturnFullResponseDtoTest() {
        String login = "tester";
        User user = createUser(login);
        String currency = "RUB";

        var setup = clearDatabase()
                .then(userRepository.save(user))
                .flatMap(u -> accountRepository.save(createAccount(u.getId(), "100.00")));

        StepVerifier.create(setup.then(accountService.getByLogin(login, currency)))
                .assertNext(dto -> {
                    assertThat(dto.getLogin()).isEqualTo(login);
                    assertThat(dto.getBalance()).isEqualByComparingTo("100.00");
                    assertThat(dto.getName()).isEqualTo("Existing User");
                })
                .verifyComplete();
    }

    /**
     * Тестирует успешное изменение баланса (пополнение).
     * Проверяет, что итоговый баланс в БД обновился корректно.
     */
    @Test
    @DisplayName("changeBalance: Успешное пополнение баланса")
    void changeBalanceDepositShouldUpdateBalanceTest() {
        String login = "deposit_user";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal delta = new BigDecimal("50.50");
        String currency = "RUB";

        var setup = clearDatabase()
                .then(userRepository.save(createUser(login)))
                .flatMap(u -> accountRepository.save(createAccount(u.getId(), initialBalance)));

        var action = setup.then(accountService.changeBalance(login, delta, currency));

        StepVerifier.create(action)
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getMessage()).isEqualTo("Баланс обновлен");
                })
                .verifyComplete();

        StepVerifier.create(userRepository.findByLogin(login)
                        .flatMap(u -> accountRepository.findByUserIdAndCurrency(u.getId(), Currency.valueOf(currency))))
                .assertNext(acc -> assertThat(acc.getBalance()).isEqualByComparingTo("150.50"))
                .verifyComplete();
    }

    /**
     * Тестирует попытку списания суммы, превышающей текущий баланс.
     * Должен возвращать ошибку INSUFFICIENT_FUNDS.
     */
    @Test
    @DisplayName("changeBalance: Ошибка при недостаточном балансе")
    void changeBalanceWithdrawShouldFailIfInsufficientTest() {
        String login = "poor_user";
        User user = createUser(login);
        String currency = "RUB";

        var setup = clearDatabase()
                .then(userRepository.save(user))
                .flatMap(u -> accountRepository.save(createAccount(u.getId(), "10.00")));

        var action = setup.then(accountService.changeBalance(login, new BigDecimal("-20.00"), currency));

        StepVerifier.create(action)
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
                })
                .verifyComplete();
    }

    /**
     * Тестирует получение списка других аккаунтов (кроме текущего).
     */
    @Test
    @DisplayName("getOtherAccounts: Должен возвращать всех пользователей, кроме текущего")
    void getOtherAccountsShouldReturnListWithoutCurrentTest() {
        var setup = clearDatabase()
                .then(userRepository.save(createUser("current")))
                .then(userRepository.save(createUser("other1")))
                .then(userRepository.save(createUser("other2")));

        StepVerifier.create(setup.thenMany(accountService.getOtherAccounts("current")))
                .expectNextCount(2)
                .verifyComplete();
    }
}
