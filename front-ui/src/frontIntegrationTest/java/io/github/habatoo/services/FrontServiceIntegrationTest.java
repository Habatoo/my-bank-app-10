package io.github.habatoo.services;

import io.github.habatoo.BaseFrontTest;
import io.github.habatoo.dto.AccountFullResponseDto;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционное тестирование FrontServiceImpl")
class FrontServiceIntegrationTest extends BaseFrontTest {

    @BeforeEach
    void setup() {
        registry.circuitBreaker("gateway-cb").reset();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "pass");
        TestSecurityContextHolder.setContext(new SecurityContextImpl(auth));

        int mockPort = mockWebServer.getPort();

        WebClient localWebClient = WebClient.builder()
                .filter((request, next) -> {
                    java.net.URI uri = request.url();
                    if ("gateway".equals(uri.getHost())) {
                        java.net.URI newUri = org.springframework.web.util.UriComponentsBuilder.fromUri(uri)
                                .host("localhost")
                                .port(mockPort)
                                .build(true)
                                .toUri();
                        request = org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                .url(newUri)
                                .build();
                    }
                    return next.exchange(request);
                })
                .build();

        ReflectionTestUtils.setField(frontService, "webClient", localWebClient);
    }

    @Test
    @DisplayName("showMainPage: Ошибка Circuit Breaker при загрузке списка счетов")
    void showMainPageCircuitBreakerOnAccountsTest() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContextImpl securityContext = new SecurityContextImpl(auth);

        int requestsBefore = mockWebServer.getRequestCount();
        registry.circuitBreaker("gateway-cb").transitionToOpenState();

        AccountFullResponseDto accountDto = AccountFullResponseDto.builder()
                .name("Ivan")
                .balance(new BigDecimal("1000.00"))
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(accountDto)));

        StepVerifier.create(
                        frontService.showMainPage(null, "error msg")
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                .assertNext(rendering -> {
                    assertThat(rendering).isNotNull();
                    Map<String, Object> model = rendering.modelAttributes();
                    assertThat(model.get("accounts")).isEqualTo(Collections.emptyList());
                    assertThat(model.get("errors")).isEqualTo(List.of("error msg"));
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestsBefore + 1);
    }

    @Test
    @DisplayName("showMainPage: Успешная загрузка данных")
    void showMainPageSuccessTest() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContextImpl securityContext = new SecurityContextImpl(auth);

        AccountFullResponseDto accountDto = AccountFullResponseDto.builder()
                .name("Ivan")
                .balance(new BigDecimal("1000.00"))
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(accountDto)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(List.of(accountDto))));

        StepVerifier.create(
                        frontService.showMainPage("some info", null)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                .assertNext(rendering -> {
                    assertThat(rendering.view()).isEqualTo("main");
                    Map<String, Object> model = rendering.modelAttributes();
                    assertThat(model.get("name")).isEqualTo("Ivan");
                    assertThat(model.get("info")).isEqualTo("some info");
                    assertThat((List<?>) model.get("accounts")).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("showMainPage: Ошибка 500 от шлюза")
    void showMainPageGatewayErrorTest() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(frontService.showMainPage(null, null))
                .verifyError();
    }
}
