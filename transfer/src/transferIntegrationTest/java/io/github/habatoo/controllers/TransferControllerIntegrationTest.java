package io.github.habatoo.controllers;

import io.github.habatoo.TransferApplication;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для {@link TransferController}.
 * Проверяют обработку JWT (subject, preferred_username), роли доступа и вызов TransferService.
 */
@SpringBootTest(
        classes = TransferApplication.class,
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
@DisplayName("Интеграционное тестирование TransferController")
class TransferControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    @Test
    @DisplayName("POST /transfer - Успешный перевод для роли USER")
    void transferSuccessTest() {
        String senderLogin = "sender_user";
        String targetLogin = "target_user";
        BigDecimal amount = new BigDecimal("500.00");

        TransferDto responseData = TransferDto.builder()
                .login(targetLogin)
                .value(amount)
                .build();

        OperationResultDto<TransferDto> successResult = OperationResultDto.<TransferDto>builder()
                .success(true)
                .message("Перевод успешно выполнен")
                .data(responseData)
                .build();

        when(transferService.processTransferOperation(eq(senderLogin), any(TransferDto.class)))
                .thenReturn(Mono.just(successResult));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", senderLogin)))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", amount.toString())
                        .queryParam("account", targetLogin)
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.login").isEqualTo(targetLogin)
                .jsonPath("$.message").isEqualTo("Перевод успешно выполнен");
    }

    @Test
    @DisplayName("POST /transfer - Ошибка при попытке перевода отрицательной суммы")
    void transferNegativeValueTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", "any_user")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", "-100.00")
                        .queryParam("account", "target")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Сумма перевода должна быть больше нуля");
    }

    @Test
    @DisplayName("POST /transfer - Доступ разрешен для роли TRANSFER_ACCESS")
    void transferAccessRoleTest() {
        when(transferService.processTransferOperation(any(), any()))
                .thenReturn(Mono.just(OperationResultDto.<TransferDto>builder().success(true).build()));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_TRANSFER_ACCESS")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", "10.00")
                        .queryParam("account", "target")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /transfer - Отказ доступа для роли GUEST")
    void transferForbiddenTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", "100.00")
                        .queryParam("account", "target")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /transfer - Отказ без токена (Unauthorized)")
    void transferUnauthorizedTest() {
        webTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", "100.00")
                        .queryParam("account", "target")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
