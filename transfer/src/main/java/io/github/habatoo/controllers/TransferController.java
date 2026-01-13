package io.github.habatoo.controllers;

import io.github.habatoo.dto.TransferDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RestController
public class TransferController {

    @PostMapping("/transfer")
    public Mono<TransferDto> updateBalance(
            @RequestParam("value") BigDecimal value,
            @AuthenticationPrincipal Jwt jwt) {

        String login = jwt.getClaimAsString("preferred_username");
        String name = jwt.getClaimAsString("name");

        log.info("API TRANSFER: Processing value {} for user {}", value, name);

        TransferDto responseDto = TransferDto.builder()
                .login(login)
                .value(value)
                .build();

        return Mono.just(responseDto);
    }
}
