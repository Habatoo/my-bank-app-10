package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.services.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * Юнит-тесты для {@link NotificationController} с использованием WebFluxTest.
 */
@WebFluxTest(NotificationController.class)
@ContextConfiguration(classes = {
        NotificationController.class,
        SecurityChassisAutoConfiguration.class
})
@DisplayName("Тестирование контроллера уведомлений (NotificationController)")
class NotificationControllerCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    /**
     * Тест успешной обработки уведомления.
     * Проверяет статус 200 OK, структуру ответа и вызов сервиса.
     */
    @Test
    @WithMockUser(roles = "NOTIFICATION_ACCESS")
    @DisplayName("Успешная обработка уведомления")
    void handleNotificationSuccessTest() {
        NotificationEvent event = NotificationEvent.builder()
                .username("test_user")
                .message("Hello")
                .build();

        when(notificationService.processEvent(any())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    byte[] body = result.getResponseBody();
                    System.out.println("RESPONSE BODY: " + (body != null ? new String(body) : "EMPTY"));
                })
                .jsonPath("$.success").isEqualTo(true);
    }

    /**
     * Тест обработки ошибки в бизнес-логике.
     * Проверяет работу блока onErrorResume и возврат success: false.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Возврат ошибки при сбое в сервисе")
    void handleNotificationServiceErrorTest() {
        NotificationEvent event = NotificationEvent.builder().username("user1").build();
        String errorMessage = "Database connection failed";

        when(notificationService.processEvent(any(NotificationEvent.class)))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Ошибка при обработке уведомления: " + errorMessage);
    }

    /**
     * Тест безопасности: доступ запрещен.
     * Проверяет, что пользователь без ролей ADMIN или NOTIFICATION_ACCESS получит 403.
     */
    @Test
    @WithMockUser(roles = "GUEST")
    @DisplayName("Доступ запрещен для пользователя с ролью GUEST")
    void handleNotificationForbiddenTest() {
        NotificationEvent event = NotificationEvent.builder().build();

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(notificationService);
    }
}
