package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final WebClient webClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> sendMoney(TransferDto transferDto) {
        log.info("Transfer request to login: {} amount: {}", transferDto.getLogin(), transferDto.getValue());

        return webClient.post()
                .uri(uriBuilder -> getUri(transferDto, uriBuilder))
                .bodyValue(transferDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<TransferDto>>() {
                })
                .map(result -> getRedirect(transferDto, result))
                .onErrorResume(e -> {
                    log.error("Transfer error: {}", e.getMessage());
                    return Mono.just("redirect:/main?error=" + URLEncoder.encode(
                            "Ошибка перевода: " + e.getMessage(), StandardCharsets.UTF_8));
                });
    }

    private String getRedirect(TransferDto transferDto, OperationResultDto<TransferDto> result) {
        if (result.isSuccess()) {
            String msg = "Перевод пользователю + " + transferDto.getLogin() +
                    " на сумму " + transferDto.getValue() + " ₽ выполнено успешно";
            return "redirect:/main?info=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        } else {
            return "redirect:/main?error=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
        }
    }

    private URI getUri(TransferDto transferDto, UriBuilder uriBuilder) {
        return uriBuilder
                .scheme("http")
                .host("gateway")
                .path("/api/main/transfer")
                .queryParam("account", transferDto.getLogin())
                .queryParam("value", transferDto.getValue())
                .build();
    }
}
