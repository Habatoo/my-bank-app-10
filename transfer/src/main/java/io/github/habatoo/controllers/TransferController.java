package io.github.habatoo.controllers;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TRANSFER_ACCESS')")
    public Mono<OperationResultDto<TransferDto>> updateBalance(
            @RequestParam("value") BigDecimal value,
            @RequestParam("account") String targetLogin,
            @AuthenticationPrincipal Jwt jwt) {

        String senderLogin = jwt.getClaimAsString("preferred_username");

        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just(OperationResultDto.<TransferDto>builder()
                    .success(false)
                    .message("Сумма перевода должна быть больше нуля")
                    .build());
        }

        TransferDto transferDto = TransferDto.builder()
                .login(targetLogin)
                .value(value)
                .build();

        return transferService.processTransferOperation(senderLogin, transferDto);
    }
}
