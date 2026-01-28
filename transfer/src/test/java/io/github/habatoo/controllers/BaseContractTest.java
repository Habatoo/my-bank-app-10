package io.github.habatoo.controllers;

import io.github.habatoo.TransferApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                TransferApplication.class,
                TestSecurityConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @Autowired
    protected ApplicationContext context;

    protected WebTestClient webTestClient;

    @MockitoBean
    protected TransferService transferService;

    @BeforeEach
    void setup() {
        String mockSubject = "367c3cf3-af3e-44af-ab7b-6d2034c6fca6";
        UUID userId = UUID.fromString(mockSubject);
        String mockUsername = "user1";

        BigDecimal amount = new BigDecimal("100.00");
        String targetAccount = "targetUser";
        TransferDto resultDto = TransferDto.builder()
                .login(targetAccount)
                .value(amount)
                .build();
        OperationResultDto<TransferDto> successResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .message("Перевод успешно выполнен")
                .data(resultDto)
                .build();

        OperationResultDto<TransferDto> zeroResponse = OperationResultDto.<TransferDto>builder()
                .success(false)
                .message("Сумма перевода должна быть больше нуля")
                .build();

        OperationResultDto<TransferDto> errorResponse = OperationResultDto.<TransferDto>builder()
                .success(false)
                .message("Ошибка списания: Недостаточно средств")
                .data(resultDto)
                .build();

        when(transferService.processTransferOperation(eq(mockUsername), any(TransferDto.class)))
                .thenAnswer(invocation -> {
                    TransferDto dto = invocation.getArgument(1);

                    if (dto.getValue().compareTo(new BigDecimal("100.00")) == 0) {
                        return Mono.just(successResponse);
                    } else if (dto.getValue().compareTo(BigDecimal.ZERO) == 0) {
                        return Mono.just(zeroResponse);
                    }

                    return Mono.just(errorResponse);
                });

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt());

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
