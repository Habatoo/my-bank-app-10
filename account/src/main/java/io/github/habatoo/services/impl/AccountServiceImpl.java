package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Override
    public Mono<AccountFullResponseDto> getByLogin(String login) {
        return userRepository.findByLogin(login)
                .flatMap(user -> accountRepository.findByUserId(user.getId())
                        .map(acc -> AccountFullResponseDto.builder()
                                .login(user.getLogin())
                                .name(user.getName())
                                .birthDate(user.getBirthDate())
                                .balance(acc.getBalance())
                                .build()));
    }

    @Override
    public Flux<AccountShortDto> getOtherAccounts(String currentLogin) {
        return userRepository.findAllByLoginNot(currentLogin)
                .map(u -> new AccountShortDto(u.getLogin(), u.getName()));
    }

    @Override
    @Transactional
    public Mono<OperationResultDto<Void>> changeBalance(String login, BigDecimal delta) {
        return userRepository.findByLogin(login)
                .flatMap(user -> accountRepository.findByUserId(user.getId()))
                .flatMap(acc -> {
                    BigDecimal newBalance = acc.getBalance().add(delta);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.just(OperationResultDto.<Void>builder()
                                .success(false)
                                .errorCode("INSUFFICIENT_FUNDS")
                                .message("Недостаточно средств на счете")
                                .build());
                    }

                    acc.setBalance(newBalance);
                    return accountRepository.save(acc)
                            .thenReturn(OperationResultDto.<Void>builder()
                                    .success(true)
                                    .message("Баланс обновлен")
                                    .build());
                })
                .switchIfEmpty(Mono.just(OperationResultDto.<Void>builder()
                        .success(false)
                        .errorCode("ACCOUNT_NOT_FOUND")
                        .message("Счет не найден")
                        .build()));
    }
}
