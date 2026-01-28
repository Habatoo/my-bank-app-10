package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.OutboxClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестирование реализации сервиса аккаунтов {@link AccountServiceImpl}.
 * Проверяет бизнес-логику работы с балансом, фильтрацию пользователей и агрегацию данных.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса AccountServiceImpl")
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxClientService outboxClientService;

    @InjectMocks
    private AccountServiceImpl accountService;

    /**
     * Тест успешного получения полной информации об аккаунте.
     */
    @Test
    @DisplayName("Получение данных по логину: успех")
    void getByLoginSuccessTest() {
        String login = "test_user";
        String currency = "RUB";
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).login(login).name("Ivan").build();
        Account account = Account.builder().userId(userId).balance(BigDecimal.valueOf(100)).build();

        when(userRepository.findByLogin(login)).thenReturn(Mono.just(user));
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.RUB)).thenReturn(Mono.just(account));

        Mono<AccountFullResponseDto> result = accountService.getByLogin(login, currency);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getLogin().equals(login)
                        && dto.getBalance().intValue() == 100)
                .verifyComplete();
    }

    /**
     * Тест получения списка других пользователей (исключая текущего).
     */
    @Test
    @DisplayName("Получение списка других аккаунтов: успех")
    void getOtherAccountsTest() {
        String currentLogin = "me";
        User other = User.builder().login("other").name("Other User").build();

        when(userRepository.findAllByLoginNot(currentLogin)).thenReturn(Flux.just(other));

        Flux<AccountShortDto> result = accountService.getOtherAccounts(currentLogin);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getLogin().equals("other"))
                .verifyComplete();
    }

    /**
     * Тест успешного пополнения баланса.
     */
    @Test
    @DisplayName("Изменение баланса: успешное пополнение")
    void changeBalanceDepositSuccessTest() {
        String login = "user";
        String currency = "RUB";
        UUID userId = UUID.randomUUID();
        BigDecimal delta = BigDecimal.valueOf(50);
        User user = User.builder().id(userId).login(login).build();
        Account account = Account.builder().userId(userId).balance(BigDecimal.valueOf(100)).build();

        when(userRepository.findByLogin(login)).thenReturn(Mono.just(user));
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.RUB)).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(account));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<OperationResultDto<Void>> result = accountService.changeBalance(login, delta, currency);

        StepVerifier.create(result)
                .expectNextMatches(OperationResultDto::isSuccess)
                .verifyComplete();

        verify(accountRepository).save(argThat(acc -> acc.getBalance().equals(BigDecimal.valueOf(150))));
        verify(outboxClientService).saveEvent(any());
    }

    /**
     * Тест отклонения операции при попытке списать больше, чем есть на счету.
     */
    @Test
    @DisplayName("Изменение баланса: ошибка при недостаточном балансе")
    void changeBalanceInsufficientFundsTest() {
        String login = "user";
        String currency = "RUB";
        UUID userId = UUID.randomUUID();
        BigDecimal withdrawDelta = BigDecimal.valueOf(-200);
        User user = User.builder().id(userId).login(login).build();
        Account account = Account.builder().userId(userId).balance(BigDecimal.valueOf(100)).build();

        when(userRepository.findByLogin(login)).thenReturn(Mono.just(user));
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.RUB)).thenReturn(Mono.just(account));

        Mono<OperationResultDto<Void>> result = accountService.changeBalance(login, withdrawDelta, currency);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && "INSUFFICIENT_FUNDS".equals(res.getErrorCode()))
                .verifyComplete();

        verify(accountRepository, never()).save(any());
    }

    /**
     * Тест поведения, если пользователь или его счет не найдены.
     */
    @Test
    @DisplayName("Изменение баланса: ошибка, если счет не найден")
    void changeBalanceAccountNotFoundTest() {
        String login = "unknown";
        String currency = "RUB";
        when(userRepository.findByLogin(login)).thenReturn(Mono.empty());

        Mono<OperationResultDto<Void>> result = accountService.changeBalance(login, BigDecimal.ONE, currency);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && "ACCOUNT_NOT_FOUND".equals(res.getErrorCode()))
                .verifyComplete();
    }
}
