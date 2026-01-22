package io.github.habatoo.services.impl;

import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.models.Transfer;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

/**
 * Модульные тесты для сервиса {@link TransferServiceImpl}.
 * Проверяют бизнес-логику переводов, включая сценарии компенсации при сбоях.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование логики переводов (TransferServiceImpl)")
class TransferServiceImplTest {

    @Mock
    private TransfersRepository transfersRepository;
    @Mock
    private OutboxClientService outboxClientService;
    @Mock
    private CircuitBreakerRegistry registry;
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @InjectMocks
    private TransferServiceImpl transferService;

    private final String SENDER = "sender_user";
    private final String RECIPIENT = "target_user";
    private final BigDecimal AMOUNT = new BigDecimal("100.00");
    private TransferDto transferDto;

    @BeforeEach
    void setUp() {
        transferDto = TransferDto.builder()
                .login(RECIPIENT)
                .value(AMOUNT)
                .build();

        CircuitBreaker cb = CircuitBreaker.ofDefaults("account-service-cb");
        when(registry.circuitBreaker("account-service-cb")).thenReturn(cb);
    }

    /**
     * Тест успешного перевода: списание, зачисление, сохранение в БД и Outbox.
     */
    @Test
    @DisplayName("Успешный перевод: все этапы проходят корректно")
    void processTransferOperation_Success() {
        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder().success(true).build();
        mockWebClientResponse(successResponse);

        when(transfersRepository.save(any(Transfer.class))).thenReturn(Mono.just(new Transfer()));
        when(outboxClientService.saveEvent(any(NotificationEvent.class))).thenReturn(Mono.empty());

        Mono<OperationResultDto<TransferDto>> result = transferService.processTransferOperation(SENDER, transferDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess() && res.getMessage().contains("успешно"))
                .verifyComplete();

        verify(transfersRepository, times(1)).save(any(Transfer.class));
        verify(outboxClientService, times(1)).saveEvent(
                argThat(event -> event.getStatus() == EventStatus.SUCCESS));
    }

    /**
     * Тест ошибки списания: если у отправителя недостаточно средств, процесс прерывается.
     */
    @Test
    @DisplayName("Ошибка списания: недостаточно средств или ошибка счета")
    void processTransferOperation_WithdrawFailure() {
        OperationResultDto<Void> failResponse = OperationResultDto.<Void>builder()
                .success(false)
                .message("Insufficient funds")
                .build();
        mockWebClientResponse(failResponse);

        Mono<OperationResultDto<TransferDto>> result = transferService.processTransferOperation(SENDER, transferDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess() && res.getMessage().contains("Ошибка списания"))
                .verifyComplete();

        verifyNoInteractions(transfersRepository);
        verifyNoInteractions(outboxClientService);
    }

    /**
     * Тест ошибки зачисления: списание успешно, зачисление упало.
     * Должна запуститься компенсация (возврат средств отправителю).
     */
    @Test
    @DisplayName("Ошибка зачисления: запуск компенсации (Saga pattern)")
    void processTransferOperation_DepositFailure_Compensation() {
        OperationResultDto<Void> success = OperationResultDto.<Void>builder().success(true).build();
        OperationResultDto<Void> fail = OperationResultDto.<Void>builder().success(false).build();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(success))
                .thenReturn(Mono.just(fail))
                .thenReturn(Mono.just(success));

        when(outboxClientService.saveEvent(any(NotificationEvent.class))).thenReturn(Mono.empty());

        Mono<OperationResultDto<TransferDto>> result = transferService.processTransferOperation(SENDER, transferDto);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess() &&
                        res.getMessage().contains("возвращены на ваш счет"))
                .verifyComplete();

        verify(outboxClientService).saveEvent(argThat(event -> event.getStatus() == EventStatus.FAILURE));
    }

    /**
     * Вспомогательный метод для мокирования вызовов WebClient к Account Service.
     */
    private void mockWebClientResponse(OperationResultDto<Void> response) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
    }
}
