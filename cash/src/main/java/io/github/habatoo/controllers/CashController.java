package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping("/cash")
    public Mono<CashDto> updateBalance(
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") String action,
            @AuthenticationPrincipal Jwt jwt) {

        String login = jwt.getClaimAsString("preferred_username");
        String userIdStr = jwt.getSubject();

        OperationType operationType = OperationType.valueOf(action.toUpperCase());

        CashDto cashDto = CashDto.builder()
                .userId(UUID.fromString(userIdStr))
                .action(operationType)
                .value(value)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return cashService.processCashOperation(login, cashDto);
    }
}
