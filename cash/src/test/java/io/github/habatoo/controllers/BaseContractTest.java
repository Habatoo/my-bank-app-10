package io.github.habatoo.controllers;

import io.github.habatoo.CashApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.CashService;
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
                CashApplication.class,
                TestSecurityConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @MockitoBean
    private CashService cashService;

    @Autowired
    ApplicationContext context;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        String mockSubject = "367c3cf3-af3e-44af-ab7b-6d2034c6fca6";
        UUID userId = UUID.fromString(mockSubject);
        String mockUsername = "user1";

        CashDto depositResponseData = CashDto.builder()
                .userId(userId)
                .action(OperationType.PUT)
                .value(new BigDecimal("150.00"))
                .build();

        OperationResultDto<CashDto> depositResponse = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция успешно проведена и сохранена")
                .data(depositResponseData)
                .build();

        CashDto withdrawResponseData = CashDto.builder()
                .userId(userId)
                .action(OperationType.GET)
                .value(new BigDecimal("50.00"))
                .build();

        OperationResultDto<CashDto> withdrawResponse = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция успешно проведена и сохранена")
                .data(withdrawResponseData)
                .build();

        when(cashService.processCashOperation(eq(mockUsername), any(CashDto.class)))
                .thenAnswer(invocation -> {
                    CashDto dto = invocation.getArgument(1);

                    if (dto.getAction() == OperationType.PUT
                            && dto.getValue().compareTo(new BigDecimal("100.00")) == 0) {
                        return Mono.just(depositResponse);
                    } else if (dto.getAction() == OperationType.GET
                            && dto.getValue().compareTo(new BigDecimal("50.00")) == 0) {
                        return Mono.just(withdrawResponse);
                    }

                    return Mono.empty();
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
