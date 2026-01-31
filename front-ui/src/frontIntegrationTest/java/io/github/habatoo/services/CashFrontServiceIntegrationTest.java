package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционное тестирование CashFrontServiceImpl")
class CashFrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setup() {
        registry.circuitBreaker("gateway-cb").reset();
    }

    @Test
    @DisplayName("moveMoney: Успешное пополнение")
    void moveMoneySuccessTest() throws Exception {
        CashDto dto = CashDto.builder()
                .action(OperationType.PUT)
                .value(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .build();

        OperationResultDto<CashDto> response = new OperationResultDto<>(true, "Success", dto, null);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response)));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);

                    assertThat(decoded).startsWith("redirect:/main?info=");
                    assertThat(decoded).contains("Пополнение");
                    assertThat(decoded).contains("100");
                    assertThat(decoded).contains("RUB");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("moveMoney: Ошибка сервиса (Circuit Breaker OPEN)")
    void moveMoneyCircuitBreakerOpenTest() {
        registry.circuitBreaker("gateway-cb").transitionToOpenState();

        CashDto dto = createCashDto(OperationType.PUT, new BigDecimal("50.00"));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?error=");
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Сервис операций временно недоступен");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("moveMoney: Ошибка 500 от шлюза")
    void moveMoneyInternalErrorTest() {
        CashDto dto = createCashDto(OperationType.PUT, new BigDecimal("10.00"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Сервис операций временно недоступен");
                })
                .verifyComplete();
    }
}
