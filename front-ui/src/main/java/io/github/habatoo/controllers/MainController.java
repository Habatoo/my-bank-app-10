package io.github.habatoo.controllers;

import io.github.habatoo.services.FrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

/**
 * Основной контроллер пользовательского интерфейса.
 * <p>
 * Данный класс отвечает за обработку запросов к главной странице личного кабинета,
 * перенаправление пользователей и отображение основного дашборда с информацией
 * о балансе, профиле и доступных операциях.
 * </p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final FrontService frontService;

    /**
     * Обрабатывает корневой путь приложения.
     * @return {@link Mono} с объектом {@link RedirectView}, перенаправляющим пользователя на страницу /main.
     */
    @GetMapping("/")
    public Mono<RedirectView> getMainPage() {
        return Mono.just(new RedirectView("/main"));
    }

    /**
     * Формирует и отображает главную страницу личного кабинета банка.
     * <p>
     * Метод запрашивает необходимые данные (баланс, список аккаунтов, данные профиля)
     * через {@link FrontService} и передает их в слой отображения (Thymeleaf).
     * </p>
     * @param info необязательный параметр, содержащий текст информационного сообщения об успешной операции.
     *
     * @param error необязательный параметр, содержащий текст ошибки для вывода пользователю.
     * @return {@link Mono} с объектом {@link Rendering}, содержащим модель данных и имя шаблона.
     */
    @GetMapping("/main")
    public Mono<Rendering> showMainPage(
            @RequestParam(required = false) String info,
            @RequestParam(required = false) String error) {

        log.debug("Запрос главной страницы. Info: {}, Error: {}", info, error);
        return frontService.showMainPage(info, error);
    }
}
