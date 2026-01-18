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
import io.github.habatoo.services.NotificationClientService;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationClientService notificationClient;

    @Transactional
    public Mono<AccountFullResponseDto> getOrCreateUser(Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");

        return userRepository.findByLogin(login)
                .flatMap(user ->
                        accountRepository.findByUserId(user.getId())
                                .map(acc -> mapToFullDto(user, acc))
                )
                .switchIfEmpty(Mono.defer(() -> registerFromToken(jwt)));
    }

    @Override
    public Mono<AccountFullResponseDto> updateProfile(String login, UserUpdateDto dto) {
        return userRepository.findByLogin(login)
                .flatMap(user -> {
                    user.setName(dto.getName());
                    user.setBirthDate(dto.getBirthDate());
                    return userRepository.save(user);
                })
                .flatMap(user -> accountRepository.findByUserId(user.getId())
                        .flatMap(acc -> {
                            AccountFullResponseDto response = mapToFullDto(user, acc);
                            return sendProfileUpdateNotification(login, dto)
                                    .thenReturn(response);
                        }));
    }

    private Mono<Void> sendProfileUpdateNotification(String login, UserUpdateDto dto) {
        NotificationEvent event = getNotificationEvent(login, dto);

        return notificationClient.send(event)
                .onErrorResume(e -> Mono.empty());
    }

    private NotificationEvent getNotificationEvent(String login, UserUpdateDto dto) {
        return NotificationEvent.builder()
                .username(login)
                .eventType(EventType.UPDATE_PROFILE)
                .status(EventStatus.SUCCESS)
                .message("Ваши персональные данные были успешно обновлены.")
                .sourceService("accounts-service")
                .payload(Map.of(
                        "name", dto.getName(),
                        "birthDate", dto.getBirthDate()
                ))
                .build();
    }

    private Mono<AccountFullResponseDto> registerFromToken(Jwt jwt) {
        BigDecimal initialSum = getInitialSum(jwt);
        User newUser = getUsername(jwt);

        return userRepository.save(newUser)
                .flatMap(savedUser -> accountRepository.save(Account.builder()
                                .userId(savedUser.getId())
                                .balance(initialSum)
                                .build())
                        .flatMap(acc -> sendRegistrationNotification(savedUser, acc)
                                .thenReturn(mapToFullDto(savedUser, acc))));
    }

    private Mono<Void> sendRegistrationNotification(User user, Account acc) {
        NotificationEvent event = NotificationEvent.builder()
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
                .build();

        log.debug("Отправка уведомления о регистрации для пользователя: {}", user.getLogin());

        return notificationClient.send(event)
                .doOnError(e -> log.error("Ошибка отправки уведомления о регистрации: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private BigDecimal getInitialSum(Jwt jwt) {
        String initialSumStr = extractClaim(jwt, "initialSum");

        return (initialSumStr != null) ? new BigDecimal(initialSumStr) : BigDecimal.ZERO;
    }

    private User getUsername(Jwt jwt) {
        String name = jwt.getClaimAsString("given_name");
        String birthDateStr = extractClaim(jwt, "birthdate");

        return User.builder()
                .login(jwt.getClaimAsString("preferred_username"))
                .name(name != null ? name : "New User")
                .birthDate(birthDateStr != null ? LocalDate.parse(birthDateStr) : LocalDate.now())
                .build();
    }

    private String extractClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim instanceof java.util.List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
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
