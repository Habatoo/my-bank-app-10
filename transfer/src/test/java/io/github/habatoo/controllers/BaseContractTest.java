package io.github.habatoo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.util.Base64;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @Autowired
    protected ApplicationContext applicationContext;

    protected WebTestClient webTestClient;

    protected String bearerToken;

    @BeforeEach
    public void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();

        this.bearerToken = generateTestJwt();
    }


    /**
     * Добавление заголовков авторизации к запросам
     */
    protected WebTestClient.RequestHeadersSpec<?> withAuth(WebTestClient.RequestHeadersSpec<?> request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
    }

    /**
     * Генерация мок JWT для контрактных тестов.
     * В реальном проекте можно использовать библиотеку JWT или статический токен.
     */
    private String generateTestJwt() {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"test-user\"}".getBytes());
        String signature = "";
        return header + "." + payload + "." + signature;
    }
}
