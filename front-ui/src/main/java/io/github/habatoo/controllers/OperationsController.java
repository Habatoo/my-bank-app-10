package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashService;
import io.github.habatoo.services.TransferService;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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

    private final CashService cashService;
    private final TransferService transferService;
    private final UserService userService;

    /**
     * Обрабатывает операции с наличными средствами (пополнение или снятие).
     *
     * @param cashDto объект передачи данных, содержащий сумму операции и тип действия.
     * @return {@link Mono} со строкой перенаправления на главную страницу с результатом операции.
     */
    @PostMapping("/cash")
    public Mono<String> handleCash(@ModelAttribute CashDto cashDto) {
        log.debug("Обработка операции с наличными: {}", cashDto);
        return cashService.moveMoney(cashDto);
    }

    /**
     * Обрабатывает запрос на перевод денежных средств другому клиенту банка.
     *
     * @param transferDto объект передачи данных, содержащий логин получателя и сумму перевода.
     * @return {@link Mono} со строкой перенаправления, содержащей сообщение об успехе или ошибке.
     */
    @PostMapping("/transfer")
    public Mono<String> handleTransfer(@ModelAttribute TransferDto transferDto) {
        log.debug("Обработка перевода средств: {}", transferDto);
        return transferService.sendMoney(transferDto);
    }

    /**
     * Обрабатывает изменение персональных данных в профиле пользователя.
     * <p>
     * Извлекает данные формы из {@link ServerWebExchange} и инициирует обновление
     * через сервис пользователей.
     * </p>
     *
     * @param exchange текущий обмен данными сервера (контекст запроса).
     * @return {@link Mono} с объектом {@link RedirectView} для возврата на главную страницу.
     */
    @PostMapping("/account")
    public Mono<RedirectView> updateProfile(ServerWebExchange exchange) {
        log.debug("Запрос на обновление профиля пользователя");
        return userService.updateProfile(exchange);
    }
}
