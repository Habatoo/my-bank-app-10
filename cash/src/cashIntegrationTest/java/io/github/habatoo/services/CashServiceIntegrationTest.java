package io.github.habatoo.services;

import io.github.habatoo.BaseCashTest;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.impl.CashServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Интеграционный тест для {@link CashServiceImpl}.
 * Проверяет логику обработки наличных, взаимодействие с внешним Account Service через MockWebServer
 * и механизмы отката (компенсации) при ошибках БД.
 */
@DisplayName("Интеграционное тестирование CashService")
class CashServiceIntegrationTest extends BaseCashTest {

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

        ReflectionTestUtils.setField(cashService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("Успешное внесение наличных (PUT): AccountService -> DB -> Outbox")
    void processCashOperationPutSuccessTest() throws Exception {
        String login = "test_user";
        CashDto cashDto = createCashDto(OperationType.PUT, new BigDecimal("1000.00"));

        OperationResultDto<Void> accountResSuccess = OperationResultDto.<Void>builder()
                .success(true)
                .message("Операция успешно проведена и сохранена")
                .build();

        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(accountResSuccess))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(accountResSuccess))
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        var result = operationsRepository.deleteAll()
                .then(cashService.processCashOperation(login, cashDto));

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isTrue();
                    assertThat(res.getMessage()).contains("успешно проведена");
                })
                .verifyComplete();

        StepVerifier.create(operationsRepository.findAll())
                .assertNext(op -> {
                    assertThat(op.getUsername()).isEqualTo(login);
                    assertThat(op.getAmount()).isEqualByComparingTo(cashDto.getValue());
                    assertThat(op.getOperationType()).isEqualTo(OperationType.PUT);
                })
                .verifyComplete();

        verify(outboxClientService).saveEvent(argThat(event ->
                event.getStatus() == EventStatus.SUCCESS && event.getUsername().equals(login)));
    }

    @Test
    @DisplayName("Ошибка БД: Списание прошло, БД упала -> Запуск компенсации")
    void processCashOperationDatabaseErrorShouldCompensateTest() throws Exception {
        CashDto cashDto = createCashDto(OperationType.GET, new BigDecimal("500.00"));

        OperationResultDto<Void> accountRes = OperationResultDto.<Void>builder().success(true).build();
        OperationResultDto<Void> compensationRes = OperationResultDto.<Void>builder().success(true).build();

        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(accountRes))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(compensationRes))
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(reactor.core.publisher.Mono.empty());

        var result = cashService.processCashOperation(null, cashDto);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isFalse();
                    assertThat(res.getMessage()).contains("Средства возвращены");
                })
                .verifyComplete();

        verify(outboxClientService).saveEvent(argThat(event -> event.getStatus() == EventStatus.FAILURE));
    }
}