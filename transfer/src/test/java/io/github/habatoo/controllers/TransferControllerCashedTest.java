package io.github.habatoo.controllers;

import io.github.habatoo.configurations.SecurityChassisAutoConfiguration;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.TransferService;
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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Интеграционный тест для {@link TransferController} с использованием среза WebFlux.
 * Проверяет корректность работы эндпоинтов, сериализацию данных и правила безопасности.
 */
@WebFluxTest(controllers = TransferController.class)
@ContextConfiguration(classes = {
        TransferController.class,
        SecurityChassisAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционное тестирование эндпоинтов TransferController")
class TransferControllerCashedTest {

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

    /**
     * Тест проверяет успешное выполнение запроса на перевод при наличии роли USER.
     * Использует mockJwt() для эмуляции аутентифицированного пользователя.
     */
    @Test
    @DisplayName("POST /transfer - Успех при наличии роли ROLE_USER")
    void transferMoneySuccess() {
        BigDecimal amount = new BigDecimal("100.00");
        String targetAccount = "ivan_ivanov";
        String RUB = "RUB";

        TransferDto resultDto = TransferDto.builder()
                .login(targetAccount)
                .value(amount)
                .currency(Currency.RUB)
                .build();

        OperationResultDto<TransferDto> serviceResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .message("Operation completed")
                .data(resultDto)
                .build();

        when(transferService.processTransferOperation(eq("test_user"), any(TransferDto.class)))
                .thenReturn(Mono.just(serviceResponse));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", "test_user"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", amount)
                        .queryParam("account", targetAccount)
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.login").isEqualTo(targetAccount)
                .jsonPath("$.data.value").isEqualTo(100.00);
    }

    /**
     * Тест проверяет работу безопасности: доступ должен быть запрещен без нужной роли.
     */
    @Test
    @DisplayName("POST /transfer - Ошибка 403 при отсутствии необходимых ролей")
    void transferMoneyForbidden() {
        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", 100)
                        .queryParam("account", "any")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    /**
     * Тест проверяет возврат сообщения об ошибке при невалидной сумме (бизнес-валидация контроллера).
     */
    @Test
    @DisplayName("POST /transfer - Ошибка 200 с флагом success=false при сумме <= 0")
    void transferMoneyNegativeValue() {
        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", "test_user"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfer")
                        .queryParam("value", -50)
                        .queryParam("account", "target")
                        .queryParam("currency", "RUB")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Сумма перевода должна быть больше нуля");
    }
}
