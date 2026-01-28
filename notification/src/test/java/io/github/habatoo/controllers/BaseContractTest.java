package io.github.habatoo.controllers;

import io.github.habatoo.NotificationApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.services.NotificationService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                NotificationApplication.class,
                TestSecurityConfiguration.class
        }
)
public abstract class BaseContractTest {

    @Autowired
    protected ApplicationContext context;

    protected WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        when(notificationService.processEvent(any(NotificationEvent.class)))
                .thenAnswer(invocation -> {
                    NotificationEvent event = invocation.getArgument(0);

                    if ("WITHDRAW".equals(event.getEventType()) && "FAILURE".equals(event.getStatus())) {
                        return Mono.error(new RuntimeException("Недостаточно средств"));
                    }

                    if ("UNKNOWN".equals(event.getEventType()) || (event.getUsername() != null && event.getUsername().isEmpty())) {
                        throw new IllegalArgumentException("Некорректные входные данные");
                    }

                    return Mono.empty();
                });

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt()
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
