package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountFullResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class AccountController {

    @GetMapping("/user")
    public Mono<AccountFullResponseDto> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");
        String name = jwt.getClaimAsString("name");
        String externalId = jwt.getSubject();

        log.info("Fetching combined data for login: {}, subject: {}", login, externalId);

        AccountFullResponseDto response = AccountFullResponseDto.builder()
                .login(login != null ? login : "user_login")
                .name(name != null ? name : "Имя не указано")
                .birthDate(LocalDate.of(1990, 1, 1))
                .accountId(UUID.randomUUID())
                .balance(new BigDecimal("50000.0000"))
                .version(0L)
                .build();

        return Mono.just(response);
    }

    @GetMapping("/users")
    public Mono<List<AccountFullResponseDto>> getAllAccount() {

        log.info("Fetching all data ");

        AccountFullResponseDto response = AccountFullResponseDto.builder()
                .login("other_user_login")
                .name("other user")
                .birthDate(LocalDate.of(1995, 5, 5))
                .accountId(UUID.randomUUID())
                .balance(new BigDecimal("22000.0000"))
                .version(0L)
                .build();

        return Mono.just(List.of(response));
    }
}
