package io.github.habatoo.controllers;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
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
    @DisplayName("Обновление баланса: успешное создание операции PUT")
    void updateBalanceDepositSuccessTest() {
        BigDecimal amount = new BigDecimal("500.00");
        String action = "put";

        CashDto expectedDto = CashDto.builder()
                .userId(UUID.fromString(TEST_USER_ID))
                .value(amount)
                .action(OperationType.PUT)
                .build();

        OperationResultDto<CashDto> successResult = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция выполнена")
                .data(expectedDto)
                .build();

        when(cashService.processCashOperation(eq(TEST_USERNAME), any(CashDto.class)))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<CashDto>> result = cashController.updateBalance(amount, action, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess() &&
                        res.getData().getAction() == OperationType.PUT &&
                        res.getData().getValue().equals(amount))
                .verifyComplete();

        verify(cashService).processCashOperation(eq(TEST_USERNAME), argThat(dto ->
                dto.getAction() == OperationType.PUT &&
                        dto.getValue().equals(amount) &&
                        dto.getUserId().toString().equals(TEST_USER_ID)
        ));
    }

    /**
     * Тест проверяет успешную обработку операции снятия (GET).
     */
    @Test
    @DisplayName("Обновление баланса: успешное создание операции GET")
    void updateBalanceWithdrawSuccessTest() {
        BigDecimal amount = new BigDecimal("200.00");
        String action = "get";

        OperationResultDto<CashDto> successResult = OperationResultDto.<CashDto>builder()
                .success(true)
                .build();

        when(cashService.processCashOperation(eq(TEST_USERNAME), any(CashDto.class)))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<CashDto>> result = cashController.updateBalance(amount, action, jwt);

        StepVerifier.create(result)
                .expectNextMatches(OperationResultDto::isSuccess)
                .verifyComplete();

        verify(cashService).processCashOperation(eq(TEST_USERNAME), argThat(dto ->
                dto.getAction() == OperationType.GET
        ));
    }

    /**
     * Тест проверяет поведение при передаче некорректного типа операции.
     * Ожидается исключение IllegalArgumentException при вызове OperationType.valueOf().
     */
    @Test
    @DisplayName("Обновление баланса: ошибка при передаче невалидного типа операции")
    void updateBalanceInvalidActionTest() {
        BigDecimal amount = new BigDecimal("100.00");
        String invalidAction = "invalid_action";

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                cashController.updateBalance(amount, invalidAction, jwt)
        );

        verifyNoInteractions(cashService);
    }
}
