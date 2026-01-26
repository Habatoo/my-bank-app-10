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

/**
 * Контроллер для управления аккаунтами пользователей.
 * <p>
 * Предоставляет API для получения информации о пользователях системы
 * и выполнения операций с балансом. Доступ к методам ограничен ролями безопасности.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Получение списка всех аккаунтов, кроме аккаунта текущего авторизованного пользователя.
     * <p>
     * Идентификация пользователя происходит на основе утверждения (claim) "preferred_username" из JWT-токена.
     * </p>
     *
     * @param jwt объект авторизованного пользователя, содержащий данные токена.
     * @return поток {@link Flux} с краткими данными аккаунтов {@link AccountShortDto}.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Flux<AccountShortDto> getList(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        log.debug("Запрос списка аккаунтов от пользователя: {}", username);
        return accountService.getOtherAccounts(username);
    }

    /**
     * Внутренний метод для изменения баланса пользователя.
     * <p>
     * Используется администраторами или техническими сервисами для прямой корректировки средств.
     * </p>
     *
     * @param login  логин пользователя, которому необходимо изменить баланс.
     * @param amount сумма изменения (положительная для начисления, отрицательная для списания).
     * @return результат операции {@link OperationResultDto} в виде реактивного объекта {@link Mono}.
     */
    @PostMapping("/balance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Mono<OperationResultDto<Void>> updateBalanceInternal(
            @RequestParam String login,
            @RequestParam BigDecimal amount) {

        log.info("Запрос на изменение баланса для пользователя {}: {}", login, amount);
        return accountService.changeBalance(login, amount);
    }
}
