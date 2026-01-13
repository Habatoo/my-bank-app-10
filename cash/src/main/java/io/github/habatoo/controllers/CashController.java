package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.enums.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RestController
public class CashController {

    @PostMapping("/cash")
    public Mono<CashDto> updateBalance(
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") String action,
            @AuthenticationPrincipal Jwt jwt) {

        String userIdStr = jwt.getSubject();
        log.info("API CALL: Processing action {} value {} for user {}", action, value, userIdStr);

        OperationType operationType;
        try {
            operationType = OperationType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid action received: {}", action);
            return Mono.error(new IllegalArgumentException("Неверный тип операции: " + action));
        }

        CashDto responseDto = CashDto.builder()
                .userId(UUID.fromString(userIdStr))
                .action(operationType)
                .value(value)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return Mono.just(responseDto);
    }
}
