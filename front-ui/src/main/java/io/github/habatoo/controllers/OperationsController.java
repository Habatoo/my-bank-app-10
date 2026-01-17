package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.dto.enums.OperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

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
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("gateway")
                        .path("/api/main/cash")
                        .queryParam("value", cashDto.getValue())
                        .queryParam("action", cashDto.getAction())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<CashDto>>() {
                })
                .map(result -> {
                    if (result.isSuccess()) {
                        String actionName = (cashDto.getAction() == OperationType.PUT) ? "Пополнение" : "Снятие";
                        String msg = actionName + " на сумму " + cashDto.getValue() + " ₽ выполнено успешно";
                        return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
                    } else {
                        return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Системная ошибка: {}", e.getMessage());
                    return Mono.just(
                            "redirect:/main?error=" + URLEncoder.encode(
                                    "Сервис временно недоступен", StandardCharsets.UTF_8));
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
                        .queryParam("account", transferDto.getLogin())
                        .queryParam("value", transferDto.getValue())
                        .build())
                .bodyValue(transferDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<TransferDto>>() {
                })
                .map(result -> {
                    if (result.isSuccess()) {
                        String msg = "Перевод пользователю + " + transferDto.getLogin() +
                                " на сумму " + transferDto.getValue() + " ₽ выполнено успешно";
                        return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
                    } else {
                        return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Transfer error: {}", e.getMessage());
                    return Mono.just("redirect:/main?error=" + URLEncoder.encode(
                            "Ошибка перевода: " + e.getMessage(), StandardCharsets.UTF_8));
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
