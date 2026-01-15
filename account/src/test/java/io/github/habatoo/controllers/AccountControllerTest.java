package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.services.AccountService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private WebTestClient webTestClient;

    private final Jwt mockJwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "test_user")
            .build();

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(accountController)
                .argumentResolvers(configurer -> {
                    configurer.addCustomResolver(new HandlerMethodArgumentResolver() {
                        @Override
                        public boolean supportsParameter(@NotNull MethodParameter parameter) {
                            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                        }

                        @Override
                        public @NotNull Mono<Object> resolveArgument(@NotNull MethodParameter parameter,
                                                                     @NotNull BindingContext bindingContext,
                                                                     @NotNull ServerWebExchange exchange) {
                            return Mono.just(mockJwt);
                        }
                    });
                })
                .build();
    }

    @Test
    void getListShouldReturnFluxOfAccountsTest() {
        AccountShortDto otherAccount = new AccountShortDto("other_user", "Petr Petrov");

        when(accountService.getOtherAccounts("test_user")).thenReturn(Flux.just(otherAccount));

        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AccountShortDto.class)
                .contains(otherAccount);
    }

    @Test
    void updateBalanceInternalShouldReturnOkTest() {
        String login = "test_user";
        BigDecimal amount = BigDecimal.valueOf(500);

        when(accountService.changeBalance(login, amount)).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amount)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(accountService, times(1)).changeBalance(login, amount);
    }
}
