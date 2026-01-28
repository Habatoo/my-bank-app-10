package io.github.habatoo.controllers;

import io.github.habatoo.AccountApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.*;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.services.AccountService;
import io.github.habatoo.services.UserService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                AccountApplication.class,
                TestSecurityConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private UserService userService;

    @Autowired
    ApplicationContext context;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        AccountFullResponseDto mockAccountFull = new AccountFullResponseDto(
                "user1",
                "User One",
                LocalDate.parse("1990-01-01"),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                Currency.RUB,
                1L);
        AccountFullResponseDto mockAccountFullUpdate = new AccountFullResponseDto(
                "user1",
                "Updated User",
                LocalDate.parse("1990-01-01"),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                Currency.RUB,
                1L);
        AccountShortDto mockAccountShortDto1 = new AccountShortDto("user1", "User One", Currency.RUB);
        AccountShortDto mockAccountShortDto2 = new AccountShortDto("user2", "User Two", Currency.RUB);
        OperationResultDto<Void> successResponse = OperationResultDto.<Void>builder()
                .success(true)
                .message("Баланс обновлен")
                .build();

        when(accountService.changeBalance(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Mono.just(successResponse));
        when(accountService.getOtherAccounts(anyString()))
                .thenReturn(Flux.just(mockAccountShortDto1, mockAccountShortDto2));

        when(userService.getOrCreateUser(any(Jwt.class)))
                .thenReturn(Mono.just(mockAccountFull));
        when(userService.updateProfile(anyString(), any(UserUpdateDto.class)))
                .thenReturn(Mono.just(mockAccountFullUpdate));
        when(userService.updatePassword(anyString(), any(PasswordUpdateDto.class)))
                .thenReturn(Mono.just(true));

        Jwt jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("preferred_username", "user1")
                .claim("scope", "ROLE_USER")
                .build();

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt().jwt(jwt).authorities(new SimpleGrantedAuthority("ROLE_USER")));

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
