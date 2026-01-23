package io.github.habatoo.services.impl;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.models.Cash;
import io.github.habatoo.repositories.OperationsRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Тестирование реализации сервиса кассовых операций {@link CashServiceImpl}.
 * Проверяет бизнес-логику проведения платежей, сохранение в историю,
 * работу с Outbox и механизм компенсации при ошибках БД.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса CashServiceImpl")
class CashServiceImplTest {

    @Mock
    private OperationsRepository operationsRepository;

    @Mock
    private OutboxClientService outboxClientService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private CashServiceImpl cashService;

    private final String LOGIN = "test_user";

    @BeforeEach
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("cash-service-cb");
        when(circuitBreakerRegistry.circuitBreaker("cash-service-cb")).thenReturn(cb);
    }

    /**
     * Успешный сценарий пополнения баланса.
     */
    @Test
    @DisplayName("Обработка операции: успешное пополнение (PUT)")
    void processCashOperationPutSuccessTest() {
        CashDto cashDto = CashDto.builder()
                .action(OperationType.PUT)
                .value(BigDecimal.valueOf(1000))
                .build();

        OperationResultDto<Void> accountResponse = OperationResultDto.<Void>builder()
                .success(true)
                .build();

        mockWebClientPost(accountResponse);

        when(operationsRepository.save(any(Cash.class))).thenReturn(Mono.just(new Cash()));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<OperationResultDto<CashDto>> result = cashService.processCashOperation(LOGIN, cashDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess()
                        && res.getMessage().contains("успешно проведена"))
                .verifyComplete();

        verify(operationsRepository).save(any());
        verify(outboxClientService).saveEvent(argThat(event -> event.getStatus().name().equals("SUCCESS")));
    }

    /**
     * Сценарий ошибки внешнего сервиса аккаунтов.
     */
    @Test
    @DisplayName("Обработка операции: отказ сервиса аккаунтов")
    void processCashOperationAccountErrorTest() {
        CashDto cashDto = CashDto.builder()
                .action(OperationType.PUT)
                .value(BigDecimal.valueOf(1000))
                .build();

        OperationResultDto<Void> accountError = OperationResultDto.<Void>builder()
                .success(false)
                .message("Account service unavailable")
                .errorCode("SERVICE_ERROR")
                .build();

        mockWebClientPost(accountError);

        Mono<OperationResultDto<CashDto>> result = cashService.processCashOperation(LOGIN, cashDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && "SERVICE_ERROR".equals(res.getErrorCode()))
                .verifyComplete();

        verify(operationsRepository, never()).save(any());
    }

    /**
     * Сценарий компенсации: ошибка БД после успешного списания в сервисе аккаунтов.
     */
    @Test
    @DisplayName("Компенсация: ошибка БД инициирует возврат средств")
    void compensationLogicTest() {
        CashDto cashDto = CashDto.builder()
                .action(OperationType.PUT)
                .value(BigDecimal.valueOf(500))
                .build();

        OperationResultDto<Void> accountSuccess = OperationResultDto.<Void>builder().success(true).build();
        mockWebClientPost(accountSuccess);

        when(operationsRepository.save(any(Cash.class)))
                .thenReturn(Mono.error(new RuntimeException("DB Failure")));

        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<OperationResultDto<CashDto>> result = cashService.processCashOperation(LOGIN, cashDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && res.getMessage().contains("Ошибка сохранения"))
                .verifyComplete();

        verify(webClient.post(), times(2)).uri(any(Function.class));
        verify(outboxClientService).saveEvent(argThat(event -> event.getStatus().name().equals("FAILURE")));
    }

    /**
     * Вспомогательный метод для мокирования реактивной цепочки WebClient.
     */
    @SuppressWarnings("unchecked")
    private void mockWebClientPost(OperationResultDto<Void> response) {
        when(webClient.post()
                .uri(any(Function.class))
                .retrieve()
                .bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
    }
}
