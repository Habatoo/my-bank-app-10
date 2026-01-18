package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashService;
import io.github.habatoo.services.TransferService;
import io.github.habatoo.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * Тесты для проверки обработки банковских операций в OperationsController.
 * <p>
 * Проверяют корректность приема данных из форм и вызов соответствующих
 * бизнес-сервисов (Cash, Transfer, User).
 * </p>
 */
@WebFluxTest(controllers = OperationsController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Юнит-тесты контроллера операций (OperationsController)")
class OperationsControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private UserService userService;

    /**
     * Тест проверяет обработку пополнения/снятия наличных.
     * Имитирует отправку данных HTML-формы.
     */
    @Test
    @WithMockUser
    @DisplayName("Обработка операций с наличными через /cash")
    void shouldHandleCashOperationTest() {
        String expectedRedirect = "redirect:/main?info=success";
        Mockito.when(cashService.moveMoney(any(CashDto.class)))
                .thenReturn(Mono.just(expectedRedirect));

        webTestClient.mutateWith(csrf())
                .post().uri("/cash")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("value=1000&action=PUT")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main?info=success");

        Mockito.verify(cashService).moveMoney(any(CashDto.class));
    }

    /**
     * Тест проверяет обработку перевода средств другому пользователю.
     */
    @Test
    @WithMockUser
    @DisplayName("Обработка перевода средств через /transfer")
    void shouldHandleTransferOperationTest() {
        String expectedRedirect = "redirect:/main?info=transferred";
        Mockito.when(transferService.sendMoney(any(TransferDto.class)))
                .thenReturn(Mono.just(expectedRedirect));

        webTestClient.mutateWith(csrf())
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("login=user2&value=500")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main?info=transferred");

        Mockito.verify(transferService).sendMoney(any(TransferDto.class));
    }

    /**
     * Тест проверяет обновление профиля пользователя.
     */
    @Test
    @WithMockUser
    @DisplayName("Обновление профиля через /account")
    void shouldUpdateUserProfileTest() {
        Mockito.when(userService.updateProfile(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(new RedirectView("/main")));

        webTestClient.mutateWith(csrf())
                .post().uri("/account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("name=Иван&birthdate=1990-01-01")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main");

        Mockito.verify(userService).updateProfile(any(ServerWebExchange.class));
    }
}
