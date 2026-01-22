package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Юнит тесты бина SecurityAutoConfiguration и связанных бинов с WebFluxTest.
 */
@WebFluxTest
@Import(SecurityAutoConfiguration.class)
@DisplayName("Тестирование конфигурации безопасности (SecurityAutoConfiguration)")
class SecurityAutoConfigurationCashedTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("Доступ к API без токена должен возвращать 401 Unauthorized")
    void anyExchangeWithoutTokenShouldReturn401() {
        webTestClient.get()
                .uri("/transfer")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
