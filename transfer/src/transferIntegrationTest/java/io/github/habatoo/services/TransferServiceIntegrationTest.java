package io.github.habatoo.services;

import io.github.habatoo.BaseTransferTest;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.EventStatus;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Интеграционный тест для {@link TransferService}.
 * Мы НЕ используем @MockitoBean для WebClient, а подменяем его реальным экземпляром,
 * настроенным на локальный MockWebServer.
 */
@DisplayName("Интеграционное тестирование TransferService")
class TransferServiceIntegrationTest extends BaseTransferTest {

    @BeforeEach
    void setup() {
        int mockPort = mockWebServer.getPort();
        String localHost = "http://localhost:" + mockPort;

        WebClient localWebClient = WebClient.builder()
                .baseUrl(localHost)
                .build();

        ReflectionTestUtils.setField(transferService, "webClient", localWebClient);
        ReflectionTestUtils.setField(transferService, "gatewayHost", localHost);

        if (registry.circuitBreaker("transfer-service-cb") != null) {
            registry.circuitBreaker("transfer-service-cb").reset();
        }
    }

    @Test
    @DisplayName("Успешный перевод: Списание -> Зачисление -> Запись в БД -> Outbox")
    void processTransferOperationSuccessTest() throws Exception {
        String sender = "sender_user";
        String recipient = "recipient_user";
        BigDecimal amount = new BigDecimal("500.00");
        TransferDto dto = TransferDto.builder()
                .login(recipient)
                .value(amount)
                .fromCurrency(Currency.RUB)
                .toCurrency(Currency.RUB)
                .build();

        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder()
                .success(true).build();
        String jsonResponse = objectMapper.writeValueAsString(successResponse);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        var result = transfersRepository.deleteAll()
                .then(transferService.processTransferOperation(sender, dto));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isTrue();
                    assertThat(res.getMessage()).contains("Перевод выполнен");
                })
                .verifyComplete();

        StepVerifier.create(transfersRepository.findAll())
                .assertNext(t -> {
                    assertThat(t.getSenderUsername()).isEqualTo(sender);
                    assertThat(t.getTargetUsername()).isEqualTo(recipient);
                    assertThat(t.getAmount()).isEqualByComparingTo(amount);
                })
                .verifyComplete();

        verify(outboxClientService, times(1)).saveEvent(any());
    }

    @Test
    @DisplayName("Ошибка депозита: Запуск компенсации (возврат средств)")
    void processTransferOperation_CompensateScenario() throws Exception {
        String sender = "sender_user";
        BigDecimal amount = new BigDecimal("100.00");
        TransferDto dto = TransferDto.builder()
                .login("recipient")
                .value(amount)
                .fromCurrency(Currency.RUB)
                .toCurrency(Currency.RUB)
                .build();

        OperationResultDto<Void> successRes = OperationResultDto.<Void>builder()
                .success(true).build();
        OperationResultDto<Void> failRes = OperationResultDto.<Void>builder()
                .success(false).message("Limit").build();

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(successRes))
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(failRes))
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(successRes))
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        var result = transfersRepository.deleteAll()
                .then(transferService.processTransferOperation(sender, dto));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isFalse();
                    assertThat(res.getMessage()).contains("Средства возвращены");
                })
                .verifyComplete();

        StepVerifier.create(transfersRepository.count())
                .expectNext(0L)
                .verifyComplete();

        verify(outboxClientService).saveEvent(
                argThat(event -> event.getStatus() == EventStatus.FAILURE));
    }
}
