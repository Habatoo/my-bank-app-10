package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для контроллера операций с наличными {@link CashController}.
 * <p>
 * Класс проверяет логику формирования DTO внутри контроллера и корректность
 * вызова сервиса {@link CashService} с использованием моков.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты контроллера CashController")
class CashControllerTest {

    private final String TEST_USERNAME = "test_user";
    private final String RUB = "RUB";
    private final String TEST_USER_ID = UUID.randomUUID().toString();

    @Mock
    private CashService cashService;

    @InjectMocks
    private CashController cashController;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Mockito.mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("preferred_username")).thenReturn(TEST_USERNAME);
        lenient().when(jwt.getSubject()).thenReturn(TEST_USER_ID);
    }

    /**
     * Тест проверяет успешную обработку операции пополнения (PUT).
     * Проверяется корректность маппинга параметров запроса в объект CashDto.
     */
    @Test
    @DisplayName("Обновление баланса: успешное пополнение (PUT)")
    void updateBalanceDepositSuccessTest() {
        BigDecimal amount = new BigDecimal("500.00");
        String action = "put";
        String currency = "RUB";

        CashDto expectedDto = CashDto.builder()
                .userId(UUID.fromString(TEST_USER_ID))
                .value(amount)
                .action(OperationType.PUT)
                .currency(Currency.RUB)
                .build();

        OperationResultDto<CashDto> successResult = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция выполнена")
                .data(expectedDto)
                .build();

        when(cashService.processCashOperation(eq(amount), eq(action), eq(currency), eq(jwt)))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<CashDto>> result = cashController.updateBalance(amount, action, currency, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess() &&
                        res.getData().getAction() == OperationType.PUT &&
                        res.getData().getValue().equals(amount) &&
                        res.getData().getCurrency() == Currency.RUB)
                .verifyComplete();

        verify(cashService).processCashOperation(amount, action, currency, jwt);
    }

    /**
     * Тест проверяет успешную обработку операции снятия (GET).
     */
    @Test
    @DisplayName("Обновление баланса: успешное снятие (GET)")
    void updateBalanceWithdrawSuccessTest() {
        BigDecimal amount = new BigDecimal("200.00");
        String action = "get";
        String currency = "USD";

        OperationResultDto<CashDto> successResult = OperationResultDto.<CashDto>builder()
                .success(true)
                .data(CashDto.builder().action(OperationType.GET).build())
                .build();

        when(cashService.processCashOperation(eq(amount), eq(action), eq(currency), eq(jwt)))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<CashDto>> result = cashController.updateBalance(amount, action, currency, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess() &&
                        res.getData().getAction() == OperationType.GET)
                .verifyComplete();

        verify(cashService).processCashOperation(amount, action, currency, jwt);
    }

    /**
     * Тест проверяет поведение при передаче некорректного типа операции.
     * Ожидается исключение IllegalArgumentException при вызове OperationType.valueOf().
     */
    @Test
    @DisplayName("Обновление баланса: ошибка при передаче невалидного типа операции")
    void updateBalanceInvalidActionTest() {
        BigDecimal amount = new BigDecimal("100.00");
        String invalidAction = "INVALID";
        String currency = "RUB";

        when(cashService.processCashOperation(eq(amount), eq(invalidAction), eq(currency), eq(jwt)))
                .thenReturn(Mono.just(OperationResultDto.<CashDto>builder()
                        .success(false)
                        .message("Неверный формат параметров: Неподдерживаемый тип операции")
                        .build()));

        Mono<OperationResultDto<CashDto>> result = cashController
                .updateBalance(amount, invalidAction, currency, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess() &&
                        res.getMessage().contains("Неверный формат параметров"))
                .verifyComplete();

        verify(cashService).processCashOperation(amount, invalidAction, currency, jwt);
    }
}
