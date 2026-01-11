package io.github.habatoo.services;

import io.github.habatoo.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для NotificationClientService — проверяют вызловы для отправки нотификаций.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для NotificationClientService")
class NotificationClientServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private NotificationClientService notificationClient;

    @BeforeEach
    void setUp() {
        notificationClient = new NotificationClientService(webClient);
        ReflectionTestUtils.setField(notificationClient, "notificationUrl", "http://test-url");
    }

    @Test
    @DisplayName("Должен корректно вызывать WebClient для отправки уведомления")
    void shouldSendNotificationCorrectlyTest() {
        NotificationEvent event = NotificationEvent.builder().message("Test").build();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        StepVerifier.create(notificationClient.send(event))
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodySpec).bodyValue(event);
    }
}
