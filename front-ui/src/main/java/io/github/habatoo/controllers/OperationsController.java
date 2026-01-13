package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OperationsController {
    private final WebClient webClient;

    @PostMapping("/cash")
    public Mono<String> handleCash(@ModelAttribute CashDto cashDto) {
        if (cashDto.getValue() == null || cashDto.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just("redirect:/main?error=" +
                    URLEncoder.encode("Сумма должна быть больше нуля", StandardCharsets.UTF_8));
        }

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("gateway")
                        .path("/api/main/cash")
                        .queryParam("value", cashDto.getValue())
                        .queryParam("action", cashDto.getAction())
                        .build())
                .retrieve()
                .bodyToMono(CashDto.class)
                .map(res -> {
                    String actionName = (cashDto.getAction().name().equals("PUT")) ? "Пополнение" : "Снятие";
                    String msg = actionName + " на сумму " + res.getValue() + " ₽ выполнено успешно";

                    return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
                })
                .onErrorResume(e -> {
                    log.error("Ошибка при проведении операции: {}", e.getMessage());
                    String errorMsg = "Ошибка: " + e.getMessage();

                    return Mono.just("redirect:/main?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
                });
    }

    @PostMapping("/transfer")
    public Mono<String> handleTransfer(@ModelAttribute TransferDto transferDto) {
        log.info("Transfer request to login: {} amount: {}", transferDto.getLogin(), transferDto.getValue());

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("gateway")
                        .path("/api/main/transfer")
                        .queryParam("value", transferDto.getValue())
                        .build())
                .bodyValue(transferDto)
                .retrieve()
                .bodyToMono(Void.class)
                .then(Mono.just("redirect:/main?info=" + URLEncoder.encode("Перевод выполнен", StandardCharsets.UTF_8)))
                .onErrorResume(e -> {
                    log.error("Transfer error: {}", e.getMessage());
                    return Mono.just("redirect:/main?error=" + URLEncoder.encode("Ошибка перевода: " + e.getMessage(), StandardCharsets.UTF_8));
                });
    }

    @PostMapping("/account")
    public Mono<String> updateAccount(@ModelAttribute UserDto userDto) {
        return Mono.just("redirect:/main?info=Профиль обновлен");
    }
}
