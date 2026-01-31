package io.github.habatoo.controllers;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Контроллер для управления операциями денежных переводов между счетами пользователей.
 * Обеспечивает обработку входящих HTTP-запросов на перевод средств и проверку прав доступа.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Выполняет операцию перевода денежных средств от текущего аутентифицированного пользователя
     * к целевому аккаунту.
     *
     * @param transferDto то для пердачи данных по переводу
     * @param jwt         объект JWT-токена, содержащий данные об аутентифицированном отправителе.
     * @return {@link Mono}, содержащий {@link OperationResultDto} с результатом операции и данными перевода.
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('TRANSFER_ACCESS')")
    public Mono<OperationResultDto<TransferDto>> transferToClient(
            @RequestBody TransferDto transferDto,
            @AuthenticationPrincipal Jwt jwt) {

        String senderLogin = jwt.getClaimAsString("preferred_username");
        log.info("API: Внешний перевод от '{}' для '{}'", senderLogin, transferDto.getLogin());

        return transferService.processTransferOperation(senderLogin, transferDto);
    }

    /**
     * Обработка внутреннего перевода (между своими счетами)..
     *
     * @param transferDto то для пердачи данных по переводу
     * @param jwt         объект JWT-токена, содержащий данные об аутентифицированном отправителе.
     * @return {@link Mono}, содержащий {@link OperationResultDto} с результатом операции и данными перевода.
     */
    @PostMapping("/self-transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Mono<OperationResultDto<TransferDto>> transferToSelf(
            @RequestBody TransferDto transferDto,
            @AuthenticationPrincipal Jwt jwt) {

        String userLogin = jwt.getClaimAsString("preferred_username");
        log.info("API: Внутренний перевод пользователя '{}'", userLogin);

        transferDto.setLogin(userLogin);

        return transferService.processTransferOperation(userLogin, transferDto);
    }
}
