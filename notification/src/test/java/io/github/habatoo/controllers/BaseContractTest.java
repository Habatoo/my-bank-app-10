package io.github.habatoo.controllers;

import io.github.habatoo.NotificationApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.enums.EventStatus;
import io.github.habatoo.dto.enums.EventType;
import io.github.habatoo.repositories.NotificationRepository;
import io.github.habatoo.services.NotificationService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
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
        }
)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @Autowired
    protected ApplicationContext context;

    protected WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @BeforeEach
    void setup() {
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

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
