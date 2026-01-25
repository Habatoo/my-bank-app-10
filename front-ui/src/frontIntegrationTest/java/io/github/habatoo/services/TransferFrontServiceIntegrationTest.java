package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционное тестирование TransferFrontServiceImpl")
class TransferFrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setup() {
        registry.circuitBreaker("gateway-cb").reset();

        int mockPort = mockWebServer.getPort();

        WebClient localWebClient = WebClient.builder()
                .filter((request, next) -> {
                    URI uri = request.url();
                    if ("gateway".equals(uri.getHost())) {
                        URI newUri = UriComponentsBuilder.fromUri(uri)
                                .host("localhost")
                                .port(mockPort)
                                .build(true)
                                .toUri();
                        request = ClientRequest.from(request).url(newUri).build();
                    }
                    return next.exchange(request);
                })
                .build();

        ReflectionTestUtils.setField(transferFrontService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("sendMoney: Успешный перевод")
    void sendMoneySuccessTest() throws Exception {
        TransferDto dto = new TransferDto();
        dto.setLogin("receiver");
        dto.setValue(new BigDecimal("500.00"));

        OperationResultDto<TransferDto> response = new OperationResultDto<>(true, "Success", dto, null);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response)));

        StepVerifier.create(transferFrontService.sendMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?info=");
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Перевод пользователю + receiver на сумму 500.00 ₽ выполнено успешно");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMoney: Ошибка Circuit Breaker OPEN")
    void sendMoneyCircuitBreakerOpenTest() {
        int requestsBefore = mockWebServer.getRequestCount();
        registry.circuitBreaker("gateway-cb").transitionToOpenState();

        TransferDto dto = new TransferDto();
        dto.setLogin("any");
        dto.setValue(BigDecimal.ONE);

        StepVerifier.create(transferFrontService.sendMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?error=");
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Ошибка перевода");
                    assertThat(decoded).contains("CircuitBreaker 'gateway-cb' is OPEN");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    @DisplayName("sendMoney: Ошибка 500 от шлюза")
    void sendMoneyGatewayErrorTest() {
        TransferDto dto = new TransferDto();
        dto.setLogin("receiver");
        dto.setValue(new BigDecimal("100.00"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(transferFrontService.sendMoney(dto))
                .assertNext(res -> {
                    assertThat(res).contains("redirect:/main?error=");
                    String decoded = URLDecoder.decode(res, StandardCharsets.UTF_8);
                    assertThat(decoded).contains("Ошибка перевода");
                })
                .verifyComplete();
    }
}
