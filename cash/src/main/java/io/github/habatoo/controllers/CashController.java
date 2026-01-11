package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class CashController {

    /**
     * GET /user - страница пользователя.
     *
     * @return dto пользователя
     */
    @GetMapping("/cash")
    public Mono<AccountDto> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        //log.info("preferred_username {}", username);

        return Mono.just(new AccountDto());
    }
}
