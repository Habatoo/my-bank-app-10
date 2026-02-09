package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashFrontService;
import io.github.habatoo.services.TransferFrontService;
import io.github.habatoo.services.UserFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Контроллер для обработки банковских и профильных операций пользователя.
 * <p>
 * Данный класс обрабатывает POST-запросы, поступающие из экранных форм:
 * пополнение/снятие наличных, перевод средств между счетами и обновление
 * персональных данных пользователя.
 * </p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OperationsController {

    private final CashFrontService cashFrontService;
    private final TransferFrontService transferFrontService;
    private final UserFrontService userFrontService;

    /**
     * Обрабатывает операции с наличными.
     * Теперь CashDto должен содержать поле 'currency', приходящее из выпадающего списка.
     */
    @PostMapping("/cash")
    @PreAuthorize("isAuthenticated()")
    public Mono<String> handleCash(@ModelAttribute CashDto cashDto) {
        log.debug("Операция с наличными в валюте {}: {}", cashDto.getCurrency(), cashDto);
        return cashFrontService.moveMoney(cashDto);
    }

    /**
     * Перевод другому клиенту.
     * TransferDto теперь включает 'fromAccountCurrency' для выбора счета списания.
     */
    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public Mono<String> handleTransfer(@ModelAttribute TransferDto transferDto) {
        log.debug("Перевод клиенту {} со счета {}: в {} {}",
                transferDto.getLogin(), transferDto.getFromCurrency(),
                transferDto.getFromCurrency(), transferDto.getValue());
        return transferFrontService.sendMoney(transferDto);
    }

    /**
     * НОВЫЙ МЕТОД: Перевод между своими счетами.
     * Обрабатывает логику конвертации внутри аккаунта пользователя.
     */
    @PostMapping("/self-transfer")
    @PreAuthorize("isAuthenticated()")
    public Mono<String> handleSelfTransfer(@ModelAttribute TransferDto transferDto) {
        log.debug("Внутренний перевод: из {} в {}",
                transferDto.getFromCurrency(), transferDto.getToCurrency());
        return transferFrontService.sendMoneyToSelf(transferDto);
    }

    /**
     * Обновление профиля.
     */
    @PostMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public Mono<RedirectView> updateProfile(ServerWebExchange exchange) {
        log.debug("Запрос на обновление профиля");
        return userFrontService.updateProfile(exchange);
    }

    /**
     * Смена пароля.
     */
    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public Mono<RedirectView> changePassword(ServerWebExchange exchange) {
        log.debug("Запрос на смену пароля");
        return userFrontService.updatePassword(exchange);
    }

    /**
     * Открытие нового счета.
     * Теперь принимает параметр из формы (RUB, USD, CNY).
     */
    @PostMapping("/open-account")
    @PreAuthorize("isAuthenticated()")
    public Mono<RedirectView> openAccount(ServerWebExchange exchange) {
        return userFrontService.openNewAccount(exchange);
    }
}
