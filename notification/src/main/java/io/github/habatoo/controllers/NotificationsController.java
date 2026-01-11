package io.github.habatoo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class NotificationController {

    /**
     * GET /cash - страница пополнения / снятия со счета.
     *
     * @return  шаблон "main.html"
     */
    @GetMapping("/cash")
    public Mono<String> getSignupPage() {
        return Mono.just("main");
    }
}
