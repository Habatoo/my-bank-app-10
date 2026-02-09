package io.github.habatoo.services;

import io.github.habatoo.BaseCashTest;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.services.impl.CashServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.UUID;

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
        String fullName = "Test User Name";
        BigDecimal value = new BigDecimal("1000.00");
        String action = "PUT";
        String currency = "RUB";

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(login);
        when(jwt.getClaimAsString("name")).thenReturn(fullName);
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());

        OperationResultDto<Void> accountResSuccess = OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(accountResSuccess))
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        var testFlow = operationsRepository.deleteAll()
                .then(cashService.processCashOperation(value, action, currency, jwt))
                .flatMap(res -> {
                    assertThat(res.isSuccess()).as("Результат операции должен быть успешным").isTrue();

                    return operationsRepository.findAll().collectList()
                            .map(list -> {
                                assertThat(list).hasSize(1);
                                assertThat(list.getFirst().getUsername()).isEqualTo(login);
                                return res;
                            });
                });

        StepVerifier.create(testFlow)
                .expectNextMatches(OperationResultDto::isSuccess)
                .verifyComplete();

        verify(outboxClientService).saveEvent(argThat(event ->
                event.getStatus() == EventStatus.SUCCESS && event.getUsername().equals(login)));
    }

    @Test
    @DisplayName("Ошибка БД: Списание прошло, БД упала -> Запуск компенсации")
    void processCashOperationDatabaseErrorShouldCompensateTest() throws Exception {
        String login = "test_user";
        BigDecimal value = new BigDecimal("500.00");

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("preferred_username", login)
                .claim("name", "Test User")
                .subject("test-subject-uuid")
                .build();

        OperationResultDto<Void> accountRes = OperationResultDto.<Void>builder().success(true).build();
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(accountRes))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(accountRes))
                .addHeader("Content-Type", "application/json"));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());
        try {
            doReturn(Mono.error(new RuntimeException("DB Error")))
                    .when(operationsRepository).save(any());
        } catch (Exception e) {
        }

        var result = cashService.processCashOperation(value, "GET", "RUB", jwt);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.isSuccess()).isFalse();
                    assertThat(res.getMessage()).contains("test-subject-uuid");
                })
                .verifyComplete();
    }
}