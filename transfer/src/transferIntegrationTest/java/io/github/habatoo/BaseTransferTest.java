package io.github.habatoo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habatoo.base.BaseTest;
import io.github.habatoo.models.Transfer;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.OutboxClientService;
import io.github.habatoo.services.OutboxService;
import io.github.habatoo.services.TransferService;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@ActiveProfiles("test")
@SpringBootTest(
        classes = TransferApplication.class,
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
public abstract class BaseTransferTest extends BaseTest {

    protected static MockWebServer mockWebServer;

    @Autowired
    protected TransferService transferService;

    @Autowired
    protected TransfersRepository transfersRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OutboxService outboxService;

    @MockitoBean
    protected OutboxClientService outboxClientService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(0);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void specificProperties(DynamicPropertyRegistry registry) {
        registry.add("services.gateway.host", () -> "localhost");
        registry.add("services.gateway.port", () -> mockWebServer.getPort());
        registry.add("spring.cloud.loadbalancer.enabled", () -> "false");
        registry.add("spring.liquibase.change-log",
                () -> "classpath:db/changelog/transfer/db.changelog-master.yaml");
    }

    protected Mono<Void> clearDatabase() {
        return transfersRepository.deleteAll();
    }

    protected Transfer createTransfer(
            String senderUsername,
            String targetUsername,
            BigDecimal amount) {
        return Transfer.builder()
                .senderUsername(senderUsername)
                .targetUsername(targetUsername)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
