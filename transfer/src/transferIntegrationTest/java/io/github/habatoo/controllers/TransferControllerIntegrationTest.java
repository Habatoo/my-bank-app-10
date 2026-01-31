package io.github.habatoo.controllers;

import io.github.habatoo.TransferApplication;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Test
    @DisplayName("POST /transfer - Успешный перевод для роли USER")
    void transferSuccessTest() {
        String senderLogin = "sender_user";
        String targetLogin = "target_user";
        BigDecimal amount = new BigDecimal("500.00");

        TransferDto requestDto = TransferDto.builder()
                .login(targetLogin)
                .value(amount)
                .fromCurrency(Currency.RUB)
                .build();

        OperationResultDto<TransferDto> successResult = OperationResultDto.<TransferDto>builder()
                .success(true)
                .message("Перевод успешно выполнен")
                .data(requestDto)
                .build();

        when(transferService.processTransferOperation(eq(senderLogin), any(TransferDto.class)))
                .thenReturn(Mono.just(successResult));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", senderLogin)))
                .post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Перевод успешно выполнен");
    }

    @Test
    @DisplayName("POST /transfer - Ошибка валидации суммы (если валидация в сервисе)")
    void transferNegativeValueTest() {
        String senderLogin = "any_user";
        TransferDto negativeDto = TransferDto.builder()
                .login("target")
                .value(new BigDecimal("-100.00"))
                .build();

        when(transferService.processTransferOperation(any(), any()))
                .thenReturn(Mono.just(OperationResultDto.<TransferDto>builder()
                        .success(false)
                        .message("Сумма перевода должна быть больше нуля")
                        .build()));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", senderLogin)))
                .post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(negativeDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Сумма перевода должна быть больше нуля");
    }

    @Test
    @DisplayName("POST /self-transfer - Успешный внутренний перевод")
    void selfTransferSuccessTest() {
        String userLogin = "my_login";
        TransferDto selfDto = TransferDto.builder()
                .value(new BigDecimal("100.00"))
                .build();

        when(transferService.processTransferOperation(eq(userLogin), any()))
                .thenReturn(Mono.just(OperationResultDto.<TransferDto>builder().success(true).build()));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt.claim("preferred_username", userLogin)))
                .post()
                .uri("/self-transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(selfDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("POST /transfer - Отказ доступа для роли GUEST")
    void transferForbiddenTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TransferDto())
                .exchange()
                .expectStatus().isForbidden();
    }
}
