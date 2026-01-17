package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final WebClient webClient;

    @Override
    public Mono<CashDto> processCashOperation(String login, CashDto cashDto) {
        BigDecimal amountChange = cashDto.getAction() == OperationType.PUT
                ? cashDto.getValue()
                : cashDto.getValue().negate();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host("gateway") // или напрямую к account, если в одной сети
                        .path("/api/account/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amountChange)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .then(sendNotification(login, cashDto))
                .thenReturn(cashDto);
    }

    private Mono<Void> sendNotification(String login, CashDto dto) {
        String message = String.format("Операция %s на сумму %.2f выполнена успешно",
                dto.getAction(), dto.getValue());

        log.info("Для пользователя {} {}", login, message);

        return Mono.empty();
    }
}
