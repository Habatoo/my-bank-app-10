package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void getByLoginSuccessTest() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .login("test_user")
                .name("Ivan")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        Account account = Account.builder()
                .id(userId)
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(userRepository.findByLogin("test_user")).thenReturn(Mono.just(user));
        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.getByLogin("test_user"))
                .expectNextMatches(dto ->
                        dto.getLogin().equals("test_user") &&
                                dto.getBalance().equals(BigDecimal.valueOf(1000)))
                .verifyComplete();
    }

    @Test
    void getOtherAccountsSuccessTest() {
        User user1 = User.builder().login("user1").name("Name1").build();
        User user2 = User.builder().login("user2").name("Name2").build();

        when(userRepository.findAllByLoginNot("current")).thenReturn(Flux.just(user1, user2));

        StepVerifier.create(accountService.getOtherAccounts("current"))
                .expectNext(new AccountShortDto("user1", "Name1"))
                .expectNext(new AccountShortDto("user2", "Name2"))
                .verifyComplete();
    }

    @Test
    void changeBalancePositiveDeltaSuccessTest() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).login("user").build();
        Account account = Account.builder().id(userId).balance(BigDecimal.valueOf(100)).build();

        when(userRepository.findByLogin("user")).thenReturn(Mono.just(user));
        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.changeBalance("user", BigDecimal.valueOf(50)))
                .verifyComplete();

        verify(accountRepository).save(argThat(acc -> acc.getBalance().equals(BigDecimal.valueOf(150))));
    }

    @Test
    void changeBalanceInsufficientFundsThrowsErrorTest() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).login("user").build();
        Account account = Account.builder().id(userId).balance(BigDecimal.valueOf(10)).build();

        when(userRepository.findByLogin("user")).thenReturn(Mono.just(user));
        when(accountRepository.findByUserId(userId)).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.changeBalance("user", BigDecimal.valueOf(-50)))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getReason().equals("Insufficient funds"))
                .verify();

        verify(accountRepository, never()).save(any());
    }

    @Test
    void changeBalanceAccountNotFoundThrowsErrorTest() {
        when(userRepository.findByLogin("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(accountService.changeBalance("unknown", BigDecimal.valueOf(100)))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getReason().equals("Account not found"))
                .verify();
    }
}
