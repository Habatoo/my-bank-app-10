package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.OperationType;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционное тестирование CashFrontServiceImpl")
class CashFrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setupWebClient() {
        registry.circuitBreaker("gateway-cb").reset();

        int mockPort = mockWebServer.getPort();

        HttpClient httpClient = HttpClient.create()
                .resolver(nameResolverSpec -> nameResolverSpec
                        .hostsFileEntriesResolver((inetAddress, hostsFileEntries) ->
                                "gateway".equals(inetAddress) ? InetAddress.getLoopbackAddress() : null
                        ));

        WebClient localWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://gateway:" + mockPort)
                .build();

        ReflectionTestUtils.setField(cashFrontService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("moveMoney: Успешное пополнение")
    void moveMoneySuccessTest() throws Exception {
        CashDto dto = createCashDto(OperationType.PUT, new BigDecimal("100.00"));
        OperationResultDto<CashDto> response = new OperationResultDto<>(true, "Success", dto, null);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response)));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?info=");
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Пополнение на сумму 100.00  ₽ выполнено успешно");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("moveMoney: Ошибка сервиса (Circuit Breaker OPEN)")
    void moveMoneyCircuitBreakerOpenTest() {
        int requestsBefore = mockWebServer.getRequestCount();

        registry.circuitBreaker("gateway-cb").transitionToOpenState();

        CashDto dto = createCashDto(OperationType.PUT, new BigDecimal("50.00"));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?error=");
                    assertThat(URLDecoder.decode(res, StandardCharsets.UTF_8)).contains("Сервис временно недоступен");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    @DisplayName("moveMoney: Ошибка 500 от шлюза")
    void moveMoneyInternalErrorTest() {
        CashDto dto = createCashDto(OperationType.PUT, new BigDecimal("10.00"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(cashFrontService.moveMoney(dto))
                .assertNext(res -> {
                    assertThat(URLDecoder.decode(res, StandardCharsets.UTF_8)).contains("Сервис временно недоступен");
                })
                .verifyComplete();
    }
}
