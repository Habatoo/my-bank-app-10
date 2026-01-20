package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.models.Account;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.AccountService;
import io.github.habatoo.services.OutboxClientService;
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
    private final OutboxClientService outboxClientService;

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
                .flatMap(account -> processBalanceChange(account, login, delta))
                .switchIfEmpty(Mono.just(createErrorResponse("ACCOUNT_NOT_FOUND", "Счет не найден")));
    }

    /**
     * Логика проверки и запуска процесса обновления баланса.
     */
    private Mono<OperationResultDto<Void>> processBalanceChange(Account account, String login, BigDecimal delta) {
        BigDecimal newBalance = account.getBalance().add(delta);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.just(createErrorResponse("INSUFFICIENT_FUNDS", "Недостаточно средств"));
        }
        return updateAccountAndLogEvent(account, login, delta, newBalance);
    }

    private Mono<OperationResultDto<Void>> updateAccountAndLogEvent(
            Account account, String login, BigDecimal delta, BigDecimal newBalance) {

        account.setBalance(newBalance);
        NotificationEvent event = getNotificationEvent(login, delta, newBalance);

        return accountRepository.save(account)
                .then(outboxClientService.saveEvent(event))
                .thenReturn(buildSuccessResponse());
    }

    private OperationResultDto<Void> buildSuccessResponse() {
        return OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();
    }

    private OperationResultDto<Void> createErrorResponse(String code, String msg) {
        return OperationResultDto.<Void>builder()
                .success(false)
                .errorCode(code)
                .message(msg)
                .build();
    }

    private NotificationEvent getNotificationEvent(String login, BigDecimal delta, BigDecimal currentBalance) {
        boolean isDeposit = delta.compareTo(BigDecimal.ZERO) >= 0;
        return NotificationEvent.builder()
                .username(login)
                .eventType(isDeposit ? EventType.DEPOSIT : EventType.WITHDRAW)
                .status(EventStatus.SUCCESS)
                .message(String.format("Баланс пользователя %s изменен на %s", login, delta))
                .sourceService("accounts-service")
                .payload(Map.of(
                        "amount", delta.abs(),
                        "totalBalance", currentBalance
                ))
                .build();
    }
}
