package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.AccountService;
import io.github.habatoo.services.NotificationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationClientService notificationClient;

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
                            .then(sendBalanceNotification(login, delta, newBalance))
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

    private Mono<Void> sendBalanceNotification(String login, BigDecimal delta, BigDecimal currentBalance) {
        boolean isDeposit = delta.compareTo(BigDecimal.ZERO) >= 0;
        NotificationEvent event = getNotificationEvent(login, delta, currentBalance, isDeposit);

        return notificationClient.send(event)
                .doOnError(e -> log.error("Ошибка отправки уведомления для {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private NotificationEvent getNotificationEvent(String login, BigDecimal delta, BigDecimal currentBalance, boolean isDeposit) {
        return NotificationEvent.builder()
                .username(login)
                .eventType(isDeposit ? EventType.DEPOSIT : EventType.WITHDRAW)
                .status(EventStatus.SUCCESS)
                .message(String.format("Баланс пользователя %s успешно изменен на %s", login, delta))
                .sourceService("accounts-service")
                .payload(Map.of(
                        "amount", delta.abs(),
                        "totalBalance", currentBalance,
                        "operationType", isDeposit ? "DEPOSIT" : "WITHDRAWAL",
                        "currency", "RUB"
                ))
                .build();
    }
}
