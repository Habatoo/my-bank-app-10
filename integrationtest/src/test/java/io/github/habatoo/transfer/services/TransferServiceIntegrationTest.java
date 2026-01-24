package io.github.habatoo.transfer.services;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.services.TransferService;
import io.github.habatoo.transfer.BaseTransferTest;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;

import reactor.netty.http.client.HttpClient;

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
    void setupWebClient() {
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

        ReflectionTestUtils.setField(transferService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("Успешный перевод: Списание -> Зачисление -> Запись в БД -> Outbox")
    void processTransferOperation_SuccessScenario() throws Exception {
        String sender = "sender_user";
        String recipient = "recipient_user";
        BigDecimal amount = new BigDecimal("500.00");
        TransferDto dto = new TransferDto(recipient, amount);

        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder().success(true).build();
        String jsonResponse = objectMapper.writeValueAsString(successResponse);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(reactor.core.publisher.Mono.empty());

        var result = transfersRepository.deleteAll()
                .then(transferService.processTransferOperation(sender, dto));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isTrue();
                    assertThat(res.getMessage()).contains("успешно");
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
        TransferDto dto = new TransferDto("recipient", amount);

        OperationResultDto<Void> successRes = OperationResultDto.<Void>builder().success(true).build();
        OperationResultDto<Void> failRes = OperationResultDto.<Void>builder().success(false).message("Limit exceeded").build();

        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(successRes))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(failRes))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(successRes))
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(reactor.core.publisher.Mono.empty());

        var result = transfersRepository.deleteAll()
                .then(transferService.processTransferOperation(sender, dto));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isFalse();
                    assertThat(res.getMessage()).contains("Деньги возвращены");
                })
                .verifyComplete();

        StepVerifier.create(transfersRepository.count())
                .expectNext(0L)
                .verifyComplete();

        verify(outboxClientService).saveEvent(argThat(event -> event.getStatus() == EventStatus.FAILURE));
    }
}
