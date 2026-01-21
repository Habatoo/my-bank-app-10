package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.services.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Flux<AccountShortDto> getList(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getOtherAccounts(jwt.getClaimAsString("preferred_username"));
    }

    @PostMapping("/balance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Mono<OperationResultDto<Void>> updateBalanceInternal(
            @RequestParam String login,
            @RequestParam BigDecimal amount) {

        return accountService.changeBalance(login, amount);
    }
}
