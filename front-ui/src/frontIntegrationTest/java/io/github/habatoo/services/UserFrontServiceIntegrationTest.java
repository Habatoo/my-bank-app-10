package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.util.UriUtils.decode;

@DisplayName("Интеграционное тестирование UserFrontService")
class UserFrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setup() {
        if (registry.circuitBreaker("gateway-cb") != null) {
            registry.circuitBreaker("gateway-cb").reset();
        }

        WebClient localWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + mockWebServer.getPort())
                .build();

        ReflectionTestUtils.setField(userFrontService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("updateProfile: Успешное обновление")
    void updateProfileSuccessTest() {
        String body = "name=Ivan&birthdate=1990-01-01";

        MockServerHttpRequest request = MockServerHttpRequest.post("/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        StepVerifier.create(userFrontService.updateProfile(exchange))
                .assertNext(redirectView -> {
                    String decodedUrl = URLDecoder.decode(redirectView.getUrl(), StandardCharsets.UTF_8);
                    assertThat(decodedUrl).isEqualTo("/main?info=Профиль обновлен");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProfile: Ошибка Circuit Breaker OPEN")
    void updateProfileCircuitBreakerOpenTest() {
        int requestsBefore = mockWebServer.getRequestCount();
        registry.circuitBreaker("gateway-cb").transitionToOpenState();

        String body = "name=Ivan&birthdate=1990-01-01";
        MockServerHttpRequest request = MockServerHttpRequest.post("/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(userFrontService.updateProfile(exchange))
                .assertNext(redirectView -> {
                    String decodedUrl = URLDecoder.decode(redirectView.getUrl(), StandardCharsets.UTF_8);
                    assertThat(decodedUrl).contains("error=Ошибка обновления");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestsBefore);
    }

    @Test
    @DisplayName("updateProfile: Отсутствующие поля формы")
    void updateProfileMissingFieldsTest() {
        String body = "name=Ivan";

        MockServerHttpRequest request = MockServerHttpRequest.post("/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(userFrontService.updateProfile(exchange))
                .assertNext(redirectView -> {
                    String decodedUrl = URLDecoder.decode(redirectView.getUrl(), StandardCharsets.UTF_8);
                    assertThat(decodedUrl).contains("error=Данные не заполнены");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }
}
