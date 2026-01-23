package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики работы сервиса операций с фронтом (FrontServiceImpl).
 * <p>
 * Проверяют корректность формирования запросов к API через WebClient,
 * обработку успешных ответов и сценарии возникновения ошибок.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты фронт-сервиса (FrontServiceImpl)")
class FrontServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private FrontServiceImpl frontService;

    private SecurityContext securityContext;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("gateway-cb");
        when(circuitBreakerRegistry.circuitBreaker("gateway-cb")).thenReturn(cb);

        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        securityContext = new SecurityContextImpl(auth);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Успешная загрузка данных главной страницы")
    void shouldReturnRenderingWithFullData() {
        AccountFullResponseDto mockAccount = AccountFullResponseDto.builder()
                .name("Тестовый Пользователь")
                .balance(new BigDecimal("1000.00"))
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        when(responseSpec.bodyToMono(AccountFullResponseDto.class)).thenReturn(Mono.just(mockAccount));
        when(responseSpec.bodyToFlux(AccountFullResponseDto.class)).thenReturn(Flux.just(mockAccount));

        Mono<Rendering> result = frontService.showMainPage("Info message", null);

        StepVerifier.create(result.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
                .assertNext(rendering -> {
                    assertEquals("main", rendering.view());
                    assertEquals("Тестовый Пользователь", rendering.modelAttributes().get("name"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Обработка ошибки списка аккаунтов")
    void shouldHandleAllAccountsErrorGracefully() {
        AccountFullResponseDto mockAccount = AccountFullResponseDto.builder().name("User").build();

        when(responseSpec.bodyToMono(AccountFullResponseDto.class)).thenReturn(Mono.just(mockAccount));
        when(responseSpec.bodyToFlux(AccountFullResponseDto.class)).thenReturn(Flux.error(new RuntimeException("API Error")));

        Mono<Rendering> result = frontService.showMainPage(null, "System Error");

        StepVerifier.create(result.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
                .assertNext(rendering -> {
                    List<?> accounts = (List<?>) rendering.modelAttributes().get("accounts");
                    assertEquals(0, accounts.size());
                    assertEquals(List.of("System Error"), rendering.modelAttributes().get("errors"));
                })
                .verifyComplete();
    }
}
