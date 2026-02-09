package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционные тесты для CashController.
 * Проверяют безопасность, парсинг параметров запроса и корректность вызова CashService.
 */
@WebFluxTest(controllers = CashController.class)
@ContextConfiguration(classes = {
        CashController.class,
        SecurityChassisAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование эндпоинтов CashController")
class CashControllerCashedTest {

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

    /**
     * Тест проверяет успешное создание операции пополнения баланса.
     */
    @Test
    @DisplayName("POST /cash - Успешное пополнение баланса (PUT)")
    void depositSuccessTest() {
        String login = "test_user";
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1000.00");
        String action = "PUT";
        String currency = "RUB";

        CashDto resultDto = CashDto.builder()
                .userId(userId)
                .action(OperationType.PUT)
                .value(amount)
                .currency(Currency.RUB)
                .build();

        OperationResultDto<CashDto> serviceResponse = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Success")
                .data(resultDto)
                .build();

        when(cashService.processCashOperation(
                eq(amount),
                eq(action),
                eq(currency),
                any(Jwt.class)))
                .thenReturn(Mono.just(serviceResponse));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt
                                .claim("preferred_username", login)
                                .subject(userId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", amount)
                        .queryParam("action", action)
                        .queryParam("currency", currency)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data.action").isEqualTo("PUT")
                .jsonPath("$.data.currency").isEqualTo("RUB")
                .jsonPath("$.data.value").isEqualTo(1000.00);

        verify(cashService).processCashOperation(eq(amount), eq(action), eq(currency), any(Jwt.class));
    }

    /**
     * Проверка безопасности: доступ запрещен пользователю без необходимых ролей.
     */
    @Test
    @DisplayName("POST /cash - Ошибка 403 (Forbidden) при отсутствии ролей")
    void cashForbiddenTest() {
        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", 100)
                        .queryParam("action", "PUT")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    /**
     * Тест проверяет поведение при передаче некорректного типа операции.
     */
    @Test
    @DisplayName("POST /cash - Ошибка при неверном значении action")
    void invalidActionTest() {
        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", "test_user"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash")
                        .queryParam("value", 100)
                        .queryParam("action", "INVALID_TYPE")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
