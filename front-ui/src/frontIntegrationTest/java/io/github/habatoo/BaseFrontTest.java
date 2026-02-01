package io.github.habatoo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habatoo.base.BaseTest;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.services.*;
import io.github.habatoo.services.impl.CashFrontServiceImpl;
import io.github.habatoo.services.impl.FrontServiceImpl;
import io.github.habatoo.services.impl.TransferFrontServiceImpl;
import io.github.habatoo.services.impl.UserFrontServiceImpl;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;

@SuppressWarnings("unchecked")
@ActiveProfiles("test")
@SpringBootTest(
        classes = {
                UserFrontServiceImpl.class,
                TransferFrontServiceImpl.class,
                FrontServiceImpl.class,
                RateClientService.class,
                RateProviderService.class,
                CashFrontServiceImpl.class,
                JacksonAutoConfiguration.class,
                WebClientAutoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false"
        })
@Import({
        io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration.class,
        BaseFrontTest.WebClientTestConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseFrontTest extends BaseTest {

    protected static MockWebServer mockWebServer;

    @Autowired
    protected UserFrontService userFrontService;

    @Autowired
    protected TransferFrontService transferFrontService;

    @Autowired
    protected FrontService frontService;

    @Autowired
    protected CashFrontService cashFrontService;

    @Autowired
    protected CircuitBreakerRegistry registry;

    @Autowired
    protected RateClientService rateClientService;

    @Autowired
    protected RateProviderService rateProviderService;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void webClientProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.application.gateway.host", () -> "http://localhost:" + mockWebServer.getPort());
    }

    protected CashDto createCashDto(
            OperationType operationType,
            BigDecimal value) {
        return CashDto.builder()
                .action(operationType)
                .value(value)
                .build();
    }

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        public WebClient webClient(WebClient.Builder builder) {
            return builder.baseUrl("http://localhost:" + mockWebServer.getPort()).build();
        }
    }
}
