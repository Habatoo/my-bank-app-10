package io.github.habatoo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/cash-unavailable")
    public Mono<ResponseEntity<String>> cashFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис кассовых операций временно недоступен. Попробуйте позже."));
    }

    @GetMapping("/account-unavailable")
    public Mono<ResponseEntity<String>> accountFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис по работче со счетом временно недоступен. Попробуйте позже."));
    }

    @GetMapping("/transfer-unavailable")
    public Mono<ResponseEntity<String>> transferFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис переводов временно недоступен. Попробуйте позже."));
    }
}
