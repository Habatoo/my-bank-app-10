package io.github.habatoo.controllers;

import io.github.habatoo.CashApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.repositories.OperationsRepository;
import io.github.habatoo.services.CashService;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                CashApplication.class,
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
public abstract class BaseContractTest {

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository;

    @MockitoBean
    private OperationsRepository operationsRepository;

    @MockitoBean
    private OutboxClientService outboxClientService;

    @MockitoBean
    private NotificationClientService notificationClientService;

    @Autowired
    protected ApplicationContext context;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        String mockSubject = "367c3cf3-af3e-44af-ab7b-6d2034c6fca6";
        UUID userId = UUID.fromString(mockSubject);
        LocalDateTime localDateTime = LocalDateTime.of(2026, 2, 2, 10, 10, 10);

        CashDto depositResponseData = CashDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .value(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .accountId(UUID.randomUUID())
                .action(OperationType.PUT)
                .version(1L)
                .createdAt(localDateTime)
                .build();

        OperationResultDto<CashDto> depositResponse = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция успешно проведена и сохранена")
                .data(depositResponseData)
                .build();

        CashDto withdrawResponseData = CashDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .value(new BigDecimal("50.00"))
                .currency(Currency.RUB)
                .action(OperationType.GET)
                .version(2L)
                .createdAt(localDateTime)
                .build();

        OperationResultDto<CashDto> withdrawResponse = OperationResultDto.<CashDto>builder()
                .success(true)
                .message("Операция успешно проведена и сохранена")
                .data(withdrawResponseData)
                .build();

        when(cashService.processCashOperation(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    BigDecimal value = invocation.getArgument(0);
                    String action = invocation.getArgument(1);
                    String currency = invocation.getArgument(2);
                    Jwt jwt = invocation.getArgument(3);

                    String username = jwt.getClaimAsString("preferred_username");

                    if ("user1".equals(username) && "RUB".equalsIgnoreCase(currency)) {
                        if ("PUT".equalsIgnoreCase(action) && value.compareTo(new BigDecimal("100.00")) == 0) {
                            return Mono.just(depositResponse);
                        }
                        if ("GET".equalsIgnoreCase(action) && value.compareTo(new BigDecimal("50.00")) == 0) {
                            return Mono.just(withdrawResponse);
                        }
                    }

                    return Mono.just(OperationResultDto.<CashDto>builder()
                            .success(false)
                            .message("No mock match")
                            .build());
                });

        when(operationsRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());
        when(notificationClientService.sendScheduled(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("preferred_username", "user1")
                .subject(mockSubject)
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
