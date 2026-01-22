package io.github.habatoo.base;

import io.github.habatoo.controllers.TransferController;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.TransferService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TransferBase.TestConfig.class)
public abstract class TransferBase {

    @Configuration
    @EnableAutoConfiguration
    @Import(TransferController.class)
    static class TestConfig { }

    @Autowired
    private TransferController transferController;

    @MockitoBean
    private TransferService transferService;

    @BeforeEach
    public void setup() {
        BigDecimal amount = new BigDecimal("100.00");
        String recipient = "recipient_user";
        String sender = "test_user";

        TransferDto transferDtoResponse = TransferDto.builder()
                .login(recipient)
                .value(amount)
                .build();

        OperationResultDto<TransferDto> serviceResponse = OperationResultDto.<TransferDto>builder()
                .success(true)
                .message("Перевод успешно выполнен")
                .data(transferDtoResponse)
                .build();

        Mockito.when(transferService.processTransferOperation(
                eq(sender),
                argThat(dto -> dto.getLogin().equals(recipient) && dto.getValue().compareTo(amount) == 0)
        )).thenReturn(Mono.just(serviceResponse));

        WebTestClient webTestClient = WebTestClient
                .bindToController(transferController)
                .apply(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.claim("preferred_username", sender))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .configureClient()
                .build();

        RestAssuredWebTestClient.standaloneSetup(webTestClient);
    }
}