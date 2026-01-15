package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

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
                        .map(acc -> mapToFullDto(user, acc)));
    }

    private Mono<AccountFullResponseDto> registerFromToken(Jwt jwt) {
        String name = jwt.getClaimAsString("given_name");
        String birthDateStr = extractClaim(jwt, "birthdate");
        String initialSumStr = extractClaim(jwt, "initialSum");
        BigDecimal initialSum = (initialSumStr != null) ? new BigDecimal(initialSumStr) : BigDecimal.ZERO;

        User newUser = User.builder()
                .login(jwt.getClaimAsString("preferred_username"))
                .name(name != null ? name : "New User")
                .birthDate(birthDateStr != null ? LocalDate.parse(birthDateStr) : LocalDate.now())
                .build();

        return userRepository.save(newUser)
                .flatMap(savedUser -> accountRepository.save(Account.builder()
                                .userId(savedUser.getId())
                                .balance(initialSum)
                                .build())
                        .map(acc -> mapToFullDto(savedUser, acc)));
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
