package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashFrontService;
import io.github.habatoo.services.TransferFrontService;
import io.github.habatoo.services.UserFrontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static io.github.habatoo.dto.enums.OperationType.PUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Изолированные Unit-тесты для OperationsController.
 * Используется MockitoExtension для автоматической инициализации моков.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты OperationsController на чистых моках")
class OperationsControllerTest {

    @Mock
    private CashFrontService cashFrontService;

    @Mock
    private TransferFrontService transferFrontService;

    @Mock
    private UserFrontService userFrontService;

    @InjectMocks
    private OperationsController operationsController;

    @Test
    @DisplayName("Тест handleCash: проверка вызова cashFrontService")
    void handleCash_ShouldReturnRedirectString() {
        CashDto cashDto = CashDto.builder()
                .value(new BigDecimal("1000"))
                .action(PUT)
                .build();
        String expectedRedirect = "redirect:/main?info=success";

        when(cashFrontService.moveMoney(cashDto)).thenReturn(Mono.just(expectedRedirect));

        Mono<String> result = operationsController.handleCash(cashDto);

        StepVerifier.create(result)
                .expectNext(expectedRedirect)
                .verifyComplete();

        verify(cashFrontService, times(1)).moveMoney(cashDto);
    }

    @Test
    @DisplayName("Тест handleTransfer: проверка вызова transferFrontService")
    void handleTransfer_ShouldReturnRedirectString() {
        TransferDto transferDto = new TransferDto();
        String expectedRedirect = "redirect:/main?info=transferred";

        when(transferFrontService.sendMoney(transferDto)).thenReturn(Mono.just(expectedRedirect));

        Mono<String> result = operationsController.handleTransfer(transferDto);

        StepVerifier.create(result)
                .expectNext(expectedRedirect)
                .verifyComplete();

        verify(transferFrontService, times(1)).sendMoney(transferDto);
    }

    @Test
    @DisplayName("Тест updateProfile: проверка вызова userFrontService")
    void updateProfile_ShouldReturnRedirectView() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        RedirectView expectedView = new RedirectView("/main");

        when(userFrontService.updateProfile(exchange)).thenReturn(Mono.just(expectedView));

        Mono<RedirectView> result = operationsController.updateProfile(exchange);

        StepVerifier.create(result)
                .assertNext(view -> assertEquals("/main", view.getUrl()))
                .verifyComplete();

        verify(userFrontService, times(1)).updateProfile(exchange);
    }
}
