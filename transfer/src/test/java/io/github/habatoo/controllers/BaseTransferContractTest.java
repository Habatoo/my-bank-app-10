package io.github.habatoo.controllers;

import io.github.habatoo.TransferApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.TransferService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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
        },
        properties = {
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "chassis.security.enabled=false"
        }
)
@EnableAutoConfiguration(exclude = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
})
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseTransferContractTest {

    @Autowired
    protected ApplicationContext context;

    protected WebTestClient webTestClient;

    @MockitoBean
    protected TransferService transferService;

    @MockitoBean
    private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository;

    @MockitoBean
    private OutboxClientService outboxClientService;

    @MockitoBean
    private TransfersRepository transfersRepository;

    @BeforeEach
    void setup() {
        String mockUsername = "user1";

        BigDecimal amount = new BigDecimal("100.00");
        String targetAccount = "targetUser";
        TransferDto resultDto = TransferDto.builder()
                .login(targetAccount)
                .value(amount)
                .currency(Currency.RUB)
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

        when(transfersRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("preferred_username", "user1")
                .claim("scope", "ROLE_USER")
                .build();

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt().jwt(jwt).authorities(new SimpleGrantedAuthority("ROLE_USER")));

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
