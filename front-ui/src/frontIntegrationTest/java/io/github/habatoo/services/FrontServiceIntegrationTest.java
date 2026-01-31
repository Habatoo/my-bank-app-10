package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.UserProfileResponseDto;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционное тестирование FrontServiceImpl")
class FrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setup() {
        mockWebServer.setDispatcher(new QueueDispatcher());
        if (registry.circuitBreaker("accountServiceCB") != null) {
            registry.circuitBreaker("accountServiceCB").reset();
        }

        int mockPort = mockWebServer.getPort();

        WebClient localWebClient = WebClient.builder()
                .filter((request, next) -> {
                    URI uri = request.url();
                    if ("gateway".equals(uri.getHost())) {
                        URI newUri = UriComponentsBuilder.fromUri(uri)
                                .host("localhost")
                                .port(mockPort)
                                .build(true)
                                .toUri();
                        request = ClientRequest.from(request).url(newUri).build();
                    }
                    return next.exchange(request);
                })
                .build();

        ReflectionTestUtils.setField(frontService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("showMainPage: Успешная загрузка данных")
    void showMainPageSuccessTest() {
        UserProfileResponseDto profile = new UserProfileResponseDto();
        profile.setName("Ivan");
        profile.setLogin("ivan_cool");
        profile.setAccounts(List.of());

        AccountShortDto otherUser = new AccountShortDto();

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    if (request.getPath().contains("/main/user")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(profile));
                    } else if (request.getPath().contains("/main/users")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(List.of(otherUser)));
                    }
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500);
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        StepVerifier.create(frontService.showMainPage("some info", null))
                .assertNext(rendering -> {
                    Map<String, Object> model = rendering.modelAttributes();
                    assertThat(model.get("name")).isEqualTo("Ivan");
                    assertThat((List<?>) model.get("accounts")).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("showMainPage: Ошибка 500 от шлюза (Fallback)")
    void showMainPageGatewayErrorTest() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(frontService.showMainPage(null, null))
                .assertNext(rendering -> {
                    assertThat(rendering.view()).isEqualTo("main");
                    Map<String, Object> model = rendering.modelAttributes();

                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) model.get("errors");

                    assertThat(errors).contains("Сервис временно недоступен");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("showMainPage: Circuit Breaker OPEN")
    void showMainPageCircuitBreakerOpenTest() {
        registry.circuitBreaker("accountServiceCB").transitionToOpenState();

        StepVerifier.create(frontService.showMainPage(null, "old error"))
                .assertNext(rendering -> {
                    Map<String, Object> model = rendering.modelAttributes();
                    assertThat((List<String>) model.get("errors")).contains("Сервис временно недоступен");
                })
                .verifyComplete();
    }
}
