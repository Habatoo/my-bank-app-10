package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * {@inheritDoc}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OutboxClientService outboxClientService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Mono<AccountFullResponseDto> getOrCreateUser(Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");

        return userRepository.findByLogin(login)
                .flatMap(user -> accountRepository.findByUserId(user.getId())
                        .map(acc -> mapToFullDto(user, acc))
                )
                .switchIfEmpty(Mono.defer(() -> registerFromToken(jwt)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Mono<AccountFullResponseDto> updateProfile(String login, UserUpdateDto dto) {
        return userRepository.findByLogin(login)
                .flatMap(user -> {
                    updateUserFields(user, dto);
                    return userRepository.save(user);
                })
                .flatMap(user -> accountRepository.findByUserId(user.getId())
                        .flatMap(acc -> saveUpdateNotification(login, dto)
                                .thenReturn(mapToFullDto(user, acc))));
    }

    /**
     * Обновляет поля пользователя.
     */
    private void updateUserFields(User user, UserUpdateDto dto) {
        user.setName(dto.getName());
        user.setBirthDate(dto.getBirthDate());
    }

    /**
     * Формирует и сохраняет событие обновления профиля в Outbox.
     */
    private Mono<Void> saveUpdateNotification(String login, UserUpdateDto dto) {
        return outboxClientService.saveEvent(NotificationEvent.builder()
                .username(login)
                .eventType(EventType.UPDATE_PROFILE)
                .status(EventStatus.SUCCESS)
                .message("Ваши персональные данные были успешно обновлены.")
                .sourceService("accounts-service")
                .payload(Map.of("name", dto.getName(), "birthDate", dto.getBirthDate()))
                .build());
    }

    private Mono<Void> saveRegistrationNotification(User user, Account acc) {
        return outboxClientService.saveEvent(NotificationEvent.builder()
                .username(user.getLogin())
                .eventType(EventType.REGISTRATION)
                .status(EventStatus.SUCCESS)
                .message("Добро пожаловать! Ваш аккаунт успешно создан.")
                .sourceService("accounts-service")
                .payload(Map.of(
                        "name", user.getName(),
                        "initialBalance", acc.getBalance(),
                        "registrationDate", LocalDate.now()
                ))
                .build());
    }

    private Mono<AccountFullResponseDto> registerFromToken(Jwt jwt) {
        User newUser = buildUserFromToken(jwt);
        BigDecimal initialSum = extractInitialSum(jwt);

        return userRepository.save(newUser)
                .flatMap(savedUser -> createAccountForUser(savedUser, initialSum)
                        .flatMap(acc -> saveRegistrationNotification(savedUser, acc)
                                .thenReturn(mapToFullDto(savedUser, acc))));
    }

    private Mono<Account> createAccountForUser(User user, BigDecimal balance) {
        return accountRepository.save(Account.builder()
                .userId(user.getId())
                .balance(balance)
                .build());
    }

    private User buildUserFromToken(Jwt jwt) {
        String name = jwt.getClaimAsString("given_name");
        String birthDateStr = extractClaim(jwt, "birthdate");

        return User.builder()
                .login(jwt.getClaimAsString("preferred_username"))
                .name(name != null ? name : "New User")
                .birthDate(birthDateStr != null ? LocalDate.parse(birthDateStr) : LocalDate.now())
                .build();
    }

    private BigDecimal extractInitialSum(Jwt jwt) {
        String val = extractClaim(jwt, "initialSum");
        return (val != null) ? new BigDecimal(val) : BigDecimal.ZERO;
    }

    private String extractClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim instanceof java.util.List<?> list && !list.isEmpty()) {
            return list.getFirst().toString();
        }
        return jwt.getClaimAsString(claimName);
    }

    private AccountFullResponseDto mapToFullDto(User user, Account acc) {
        return AccountFullResponseDto.builder()
                .login(user.getLogin())
                .name(user.getName())
                .birthDate(user.getBirthDate())
                .balance(acc.getBalance())
                .build();
    }
}
