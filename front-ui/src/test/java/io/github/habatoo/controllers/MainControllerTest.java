package io.github.habatoo.controllers;

import io.github.habatoo.services.FrontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Изолированные Unit-тесты для MainController.
 * Используется MockitoExtension для автоматической инициализации моков.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты MainController на чистых моках")
class MainControllerTest {

    @Mock
    private FrontService frontService;

    @InjectMocks
    private MainController mainController;

    @Test
    @DisplayName("Тест getMainPage: проверка редиректа на /main")
    void getMainPageShouldReturnRedirectViewTest() {
        Mono<RedirectView> result = mainController.getMainPage();

        StepVerifier.create(result)
                .assertNext(redirectView -> {
                    assertEquals("/main", redirectView.getUrl());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест showMainPage: проверка вызова FrontService с параметрами")
    void showMainPageShouldCallServiceWithCorrectParamsTest() {
        String info = "success_info";
        String error = "error_msg";
        Rendering mockRendering = Rendering.view("main").build();

        when(frontService.showMainPage(info, error))
                .thenReturn(Mono.just(mockRendering));

        Mono<Rendering> result = mainController.showMainPage(info, error);

        StepVerifier.create(result)
                .expectNext(mockRendering)
                .verifyComplete();

        verify(frontService, times(1)).showMainPage(info, error);
    }

    @Test
    @DisplayName("Тест showMainPage: работа с null параметрами")
    void showMainPageShouldHandleNullParamsTest() {
        Rendering mockRendering = Rendering.view("main").build();

        when(frontService.showMainPage(null, null))
                .thenReturn(Mono.just(mockRendering));

        Mono<Rendering> result = mainController.showMainPage(null, null);

        StepVerifier.create(result)
                .expectNext(mockRendering)
                .verifyComplete();

        verify(frontService).showMainPage(null, null);
    }
}
