package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashFrontService;
import io.github.habatoo.services.TransferFrontService;
import io.github.habatoo.services.UserFrontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Тесты для проверки обработки банковских операций в OperationsController.
 * <p>
 * Проверяют корректность приема данных из форм и вызов соответствующих
 * бизнес-сервисов (Cash, Transfer, User).
 * </p>
 */
@WebFluxTest(controllers = OperationsController.class)
@ContextConfiguration(classes = {
        OperationsController.class,
        SecurityChassisAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Юнит-тесты контроллера операций (OperationsController)")
class OperationsControllerCashedTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CashFrontService cashFrontService;

    @MockitoBean
    private TransferFrontService transferFrontService;

    @MockitoBean
    private UserFrontService userFrontService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    @Test
    @DisplayName("POST /cash - Успешный редирект после операции с наличными")
    void handleCashSuccess() {
        when(cashFrontService.moveMoney(any(CashDto.class)))
                .thenReturn(Mono.just("redirect:/main?info=Success"));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt())
                .post()
                .uri("/cash")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("value=100&action=PUT")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main?info=Success");
    }

    @Test
    @DisplayName("POST /transfer - Успешный редирект после перевода средств")
    void handleTransferSuccess() {
        when(transferFrontService.sendMoney(any(TransferDto.class)))
                .thenReturn(Mono.just("redirect:/main?info=TransferDone"));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt())
                .post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("login=recipient&value=500")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main?info=TransferDone");
    }

    @Test
    @DisplayName("POST /account - Обновление профиля через ServerWebExchange")
    void updateProfileSuccess() {
        RedirectView redirectView = new RedirectView("/main?info=ProfileUpdated");

        when(userFrontService.updateProfile(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(redirectView));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt())
                .post()
                .uri("/account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("name=NewName")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main?info=ProfileUpdated");
    }

    @Test
    @DisplayName("POST /cash - Ошибка 403 при отсутствии CSRF токена")
    void handleCashNoCsrfForbidden() {
        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/cash")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("value=100")
                .exchange()
                .expectStatus().isForbidden();
    }
}
