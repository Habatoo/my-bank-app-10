package io.github.habatoo.controllers;

import io.github.habatoo.NotificationApplication;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.services.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для {@link NotificationController}.
 * Проверяют обработку JWT (subject, preferred_username), роли доступа и вызов NotificationService.
 */
@SpringBootTest(
        classes = NotificationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.liquibase.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration,org.springdoc.core.configuration.SpringDocConfiguration",
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureWebTestClient
@DisplayName("Интеграционное тестирование NotificationController")
class NotificationControllerIntegrationTest {
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

    @Test
    @DisplayName("POST /notification - Успех для роли NOTIFICATION_ACCESS")
    void handleNotificationSuccessTest() {
        NotificationEvent event = NotificationEvent.builder()
                .username("test_user")
                .eventType(EventType.TRANSFER)
                .payload(Map.of("content", "Hello World"))
                .build();

        when(notificationService.processEvent(any(NotificationEvent.class)))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_NOTIFICATION_ACCESS")))
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Уведомление принято в обработку");
    }

    @Test
    @DisplayName("POST /notification - Отказ доступа для роли USER")
    void handleNotificationForbiddenTest() {
        NotificationEvent event = NotificationEvent.builder().username("user").build();

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /notification - Обработка ошибки сервиса")
    void handleNotificationErrorTest() {
        NotificationEvent event = NotificationEvent.builder().username("error_user").build();

        when(notificationService.processEvent(any(NotificationEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Ошибка при обработке уведомления: Service failure");
    }

    @Test
    @DisplayName("POST /notification - Отказ без авторизации")
    void handleNotificationUnauthorizedTest() {
        webTestClient
                .post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NotificationEvent())
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
