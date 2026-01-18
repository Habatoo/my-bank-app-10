package io.github.habatoo.controllers;

import io.github.habatoo.services.FrontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для проверки логики отображения страниц в MainController.
 * <p>
 * Проверяют корректность редиректов и вызов методов агрегации данных во FrontService.
 * </p>
 */
@WebFluxTest(controllers = MainController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Юнит-тесты контроллера главной страницы (MainController)")
class MainControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FrontService frontService;

    /**
     * Тест проверяет автоматическое перенаправление с корневого URL на главную страницу.
     */
    @Test
    @WithMockUser
    @DisplayName("Перенаправление с '/' на '/main'")
    void shouldRedirectFromRootToMainTest() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main");
    }

    /**
     * Тест проверяет успешное отображение главной страницы без параметров.
     */
    @Test
    @WithMockUser
    @DisplayName("Отображение главной страницы без параметров")
    void shouldShowMainPageSuccessfullyTest() {
        Mockito.when(frontService.showMainPage(null, null))
                .thenReturn(Mono.just(Rendering.view("main").build()));

        webTestClient.get().uri("/main")
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(frontService).showMainPage(null, null);
    }

    /**
     * Тест проверяет передачу сообщений об ошибках и информации во FrontService.
     */
    @Test
    @WithMockUser
    @DisplayName("Отображение главной страницы с параметрами info и error")
    void shouldShowMainPageWithParametersTest() {
        String infoMsg = "Успешно";
        String errorMsg = "Ошибка";

        Mockito.when(frontService.showMainPage(eq(infoMsg), eq(errorMsg)))
                .thenReturn(Mono.just(Rendering.view("main").build()));

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/main")
                        .queryParam("info", infoMsg)
                        .queryParam("error", errorMsg)
                        .build())
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(frontService).showMainPage(infoMsg, errorMsg);
    }
}
