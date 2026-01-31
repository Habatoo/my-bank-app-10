package io.github.habatoo.controllers;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты для контроллера {@link TransferController}.
 * Проверяют логику валидации входных данных и корректность вызова сервиса переводов.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование контроллера переводов (TransferController)")
class TransferControllerTest {

    private final String RUB = "RUB";

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Mockito.mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("sender_user");
    }

    /**
     * Тест проверяет сценарий успешного перевода при валидных входных данных.
     */
    @Test
    @DisplayName("Успешный перевод: валидные данные и положительный баланс")
    void shouldReturnSuccessWhenTransferIsValidTest() {
        BigDecimal value = new BigDecimal("500.00");
        String targetAccount = "recipient_user";

        TransferDto expectedDto = TransferDto.builder()
                .login(targetAccount)
                .value(value)
                .build();

        OperationResultDto<TransferDto> successResult = OperationResultDto.<TransferDto>builder()
                .success(true)
                .data(expectedDto)
                .message("Перевод выполнен")
                .build();

        when(transferService.processTransferOperation(eq("sender_user"), any(TransferDto.class)))
                .thenReturn(Mono.just(successResult));

        Mono<OperationResultDto<TransferDto>> result = transferController.updateBalance(value, targetAccount, RUB, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> res.isSuccess()
                        && res.getData().getValue().equals(value))
                .verifyComplete();

        verify(transferService).processTransferOperation(eq("sender_user"), any(TransferDto.class));
    }

    /**
     * Тест проверяет, что перевод отклоняется, если указана отрицательная сумма.
     */
    @Test
    @DisplayName("Ошибка валидации: сумма перевода меньше или равна нулю")
    void shouldReturnErrorWhenValueIsInvalidTest() {
        BigDecimal invalidValue = new BigDecimal("-10.00");
        String targetAccount = "recipient_user";

        Mono<OperationResultDto<TransferDto>> result = transferController.updateBalance(invalidValue, targetAccount, RUB, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && res.getMessage().contains("должна быть больше нуля"))
                .verifyComplete();

        verifyNoInteractions(transferService);
    }

    /**
     * Тест проверяет поведение системы при передаче null в качестве суммы.
     */
    @Test
    @DisplayName("Ошибка валидации: сумма перевода равна null")
    void shouldReturnErrorWhenValueIsNullTest() {
        String targetAccount = "recipient_user";

        Mono<OperationResultDto<TransferDto>> result = transferController.updateBalance(null, targetAccount, RUB, jwt);

        StepVerifier.create(result)
                .expectNextMatches(res -> !res.isSuccess()
                        && res.getMessage().equals("Сумма перевода должна быть больше нуля"))
                .verifyComplete();

        verifyNoInteractions(transferService);
    }
}
