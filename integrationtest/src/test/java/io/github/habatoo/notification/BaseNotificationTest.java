package io.github.habatoo.notification;

import io.github.habatoo.AccountApplication;
import io.github.habatoo.BaseTest;
import io.github.habatoo.models.Notification;
import io.github.habatoo.repositories.NotificationRepository;
import io.github.habatoo.services.NotificationService;
import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@ActiveProfiles("test")
@SpringBootTest(
        classes = AccountApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.liquibase.enabled=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseNotificationTest extends BaseTest {

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected OutboxService outboxService;

    @MockitoBean
    protected OutboxClientService outboxClientService;

    @MockitoBean
    protected ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    protected ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @DynamicPropertySource
    static void specificProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.change-log",
                () -> "classpath:db/changelog/notification/db.changelog-master.yaml");
    }

    protected Notification createNotification(
            String name,
            String message) {
        return Notification.builder()
                .username(name)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
    }

    protected Mono<Void> clearDatabase() {
        return notificationRepository.deleteAll();
    }
}
