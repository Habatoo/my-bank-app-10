package io.github.habatoo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class TransferController {

    /**
     * GET /transfer - страница перевода со счета.
     *
     * @return  шаблон "main.html"
     */
    @GetMapping("/transfer")
    public Mono<String> getSignupPage() {
        return Mono.just("main");
    }
}
