package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
public class AccountController {

    /**
     * GET /user - страница пользователя.
     *
     * @return dto пользователя
     */
    @GetMapping("/user")
    public Mono<AccountDto> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        String fullName = jwt.getClaimAsString("name");

        log.info("Fetching account for user: {}", username);

        AccountDto account = new AccountDto();
        account.setName(fullName != null ? fullName : username);
        account.setBirthdate(LocalDate.of(1990, 1, 1));
        account.setBalance(BigDecimal.valueOf(50000));

        return Mono.just(account);
    }
}
