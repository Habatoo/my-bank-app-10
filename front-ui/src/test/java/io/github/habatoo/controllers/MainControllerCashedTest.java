package io.github.habatoo.controllers;

import io.github.habatoo.cofigurations.ThymeleafConfigurations;
import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.services.FrontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционное тестирование эндпоинтов MainController.
 * Проверяет логику перенаправлений, защиту доступа и корректность вызова фронт-сервиса.
 */
@WebFluxTest(controllers = MainController.class)
@ContextConfiguration(classes = {
        MainController.class,
        SecurityChassisAutoConfiguration.class,
        ThymeleafConfigurations.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование эндпоинтов MainController")
class MainControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FrontService frontService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    /**
     * Тест проверяет перенаправление с корневого URL на login страницу.
     */
    @Test
    @DisplayName("GET / - Редирект на страницу /login")
    void getRootRedirectTest() {
        webTestClient
                .get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");
    }

    /**
     * Тест проверяет успешное отображение главной страницы для авторизованного пользователя.
     */
    @Test
    @DisplayName("GET /main - Успешное отображение страницы для авторизованного пользователя")
    void showMainPageSuccessTest() {
        String login = "test_user";
        UUID userId = UUID.randomUUID();
        String infoMsg = "Success operation";
        Rendering rendering = Rendering.view("main-template")
                .modelAttribute("userName", "Ivan")
                .build();

        when(frontService.showMainPage(eq(infoMsg), nullable(String.class)))
                .thenReturn(Mono.just(rendering));

        webTestClient
                //.mutateWith(mockJwt())
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt
                                .claim("preferred_username", login)
                                .subject(userId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main")
                        .queryParam("info", infoMsg)
                        .build())
                .exchange()
                .expectStatus().isOk();

    }

    /**
     * Тест проверяет работу безопасности: доступ редиректит на страницу login
     */
    @Test
    @DisplayName("GET /main - Ошибка 401 (Unauthorized) без авторизации")
    void showMainPageUnauthorizedTest() {
        webTestClient
                .get()
                .uri("/main")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");
    }

    /**
     * Тест проверяет корректную передачу параметров ошибки в сервис.
     */
    @Test
    @DisplayName("GET /main - Передача сообщения об ошибке в сервис")
    void showMainPageWithErrorTest() {
        String errorMsg = "Insufficient funds";
        Rendering rendering = Rendering.view("main-template").build();

        when(frontService.showMainPage(nullable(String.class), eq(errorMsg)))
                .thenReturn(Mono.just(rendering));

        webTestClient
                .mutateWith(mockJwt())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main")
                        .queryParam("error", errorMsg)
                        .build())
                .exchange()
                .expectStatus().isOk();
    }
}
