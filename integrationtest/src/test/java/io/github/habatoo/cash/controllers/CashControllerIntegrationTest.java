package io.github.habatoo.cash.controllers;

import io.github.habatoo.CashApplication;
import io.github.habatoo.controllers.CashController;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для {@link CashController}.
 * Проверяют обработку JWT (subject, preferred_username), роли доступа и вызов CashService.
 */
@SpringBootTest(
        classes = CashApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.liquibase.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration,org.springdoc.core.configuration.SpringDocConfiguration", // 2. Исключаем SpringDoc
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureWebTestClient
@DisplayName("Интеграционное тестирование CashController")
class CashControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ErrorWebExceptionHandler errorWebExceptionHandler;

    @Test
    @DisplayName("POST /cash - Успешный депозит для роли USER")
    void depositSuccessTest() {
        String mockLogin = "user_ivan";
        UUID mockUserId = UUID.randomUUID();

        CashDto responseDto = CashDto.builder()
                .userId(mockUserId)
                .action(OperationType.PUT)
                .value(new BigDecimal("1000.00"))
                .build();

        OperationResultDto<CashDto> result = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция выполнена")
                .data(responseDto)
                .build();

        when(cashService.processCashOperation(eq(mockLogin), any(CashDto.class)))
                .thenReturn(Mono.just(result));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(jwt -> jwt
                                .claim("preferred_username", mockLogin)
                                .subject(mockUserId.toString())))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", "1000.00")
                        .queryParam("action", "PUT")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.action").isEqualTo("PUT")
                .jsonPath("$.data.userId").isEqualTo(mockUserId.toString());
    }

    @Test
    @DisplayName("POST /cash - Отказ доступа для роли GUEST")
    void forbiddenForGuestTest() {
        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", "500")
                        .queryParam("action", "GET")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /cash - Ошибка 401 при отсутствии токена")
    void unauthorizedTest() {
        webTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", "100")
                        .queryParam("action", "GET")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /cash - Успех для роли CASH_ACCESS")
    void cashAccessRoleTest() {
        String mockLogin = "cashier_1";
        UUID mockUserId = UUID.randomUUID();

        when(cashService.processCashOperation(anyString(), any(CashDto.class)))
                .thenReturn(Mono.just(OperationResultDto.<CashDto>builder().success(true).build()));

        webTestClient
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CASH_ACCESS"))
                        .jwt(jwt -> jwt
                                .claim("preferred_username", mockLogin)
                                .subject(mockUserId.toString())))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", "50.00")
                        .queryParam("action", "GET")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }
}
