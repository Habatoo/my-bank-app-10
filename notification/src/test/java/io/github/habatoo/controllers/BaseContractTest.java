package io.github.habatoo.controllers;

import io.github.habatoo.NotificationApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.repositories.NotificationRepository;
import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.NotificationService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                NotificationApplication.class,
                TestSecurityConfiguration.class
        },
        properties = {
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.liquibase.enabled=false",
                "spring.security.enabled=false",
                "spring.security.oauth2.client.registration.keycloak.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
        }
)
@EnableAutoConfiguration(exclude = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        R2dbcAutoConfiguration.class,
        R2dbcTransactionManagerAutoConfiguration.class
})
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureMessageVerifier
@AutoConfigureWebTestClient
public abstract class BaseContractTest {

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected WebTestClient webTestClient;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository;

    @BeforeEach
    void setup() {
        setupServiceMocks();
        RestAssuredWebTestClient.webTestClient(webTestClient);
    }

    private void setupServiceMocks() {
        when(notificationRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(notificationService.processEvent(any(NotificationEvent.class)))
                .thenAnswer(invocation -> {
                    NotificationEvent event = invocation.getArgument(0);

                    if (event == null) return Mono.empty();

                    if ((event.getUsername() == null || event.getUsername().isEmpty())
                            || "UNKNOWN".equals(String.valueOf(event.getEventType()))) {

                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") {
                            @Override
                            public String getReason() {
                                return "Некорректные входные данные";
                            }
                        });
                    }

                    if (event.getEventType() == EventType.WITHDRAW && event.getStatus() == EventStatus.FAILURE) {
                        return Mono.error(new RuntimeException("Недостаточно средств"));
                    }

                    return Mono.empty();
                });
    }
}
