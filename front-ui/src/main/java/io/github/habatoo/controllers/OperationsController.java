package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.UserUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OperationsController {

    private final WebClient webClient;
    private static final String BASE_GATEWAY_URL = "http://gateway/api";

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
    public Mono<RedirectView> updateProfile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String name = formData.getFirst("name");
                    String birthDateStr = formData.getFirst("birthdate");

                    if (name == null || birthDateStr == null) {
                        return Mono.just(new RedirectView("/main?error=MissingFields"));
                    }

                    UserUpdateDto updateDto = new UserUpdateDto(name, LocalDate.parse(birthDateStr));

                    return webClient.patch()
                            .uri(BASE_GATEWAY_URL + "/account/update")
                            .bodyValue(updateDto)
                            .retrieve()
                            .toBodilessEntity()
                            .map(response -> {
                                String info = UriComponentsBuilder.fromPath("")
                                        .queryParam("info", "Профиль обновлен")
                                        .build().encode().toUriString();
                                return new RedirectView("/main" + info);
                            })
                            .onErrorResume(e -> {
                                log.error("Update error: ", e);
                                return Mono.just(new RedirectView("/main?error=UpdateFailed"));
                            });
                });
    }
}
