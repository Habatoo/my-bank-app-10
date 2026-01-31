package io.github.habatoo.controllers;

import io.github.habatoo.AccountApplication;
import io.github.habatoo.dto.*;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.AccountService;
import io.github.habatoo.services.OutboxService;
import io.github.habatoo.services.UserService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AccountApplication.class,
        properties = {
                "spring.cloud.consul.enabled=false",
                "spring.cloud.consul.config.enabled=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.liquibase.enabled=false",
                "chassis.security.enabled=false"
        }
)
@EnableAutoConfiguration(exclude = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        R2dbcAutoConfiguration.class,
        R2dbcTransactionManagerAutoConfiguration.class
})
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseAccountContractTest {

    @MockitoBean private AccountService accountService;
    @MockitoBean private UserService userService;
    @MockitoBean private OutboxService outboxService;

    @MockitoBean private io.github.habatoo.repositories.AccountRepository accountRepository;
    @MockitoBean private io.github.habatoo.repositories.UserRepository userRepository;
    @MockitoBean private io.github.habatoo.repositories.OutboxRepository outboxRepository;

    @MockitoBean private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;
    @MockitoBean private ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository;
    @MockitoBean private ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService;
    @MockitoBean private ReactiveJwtDecoder reactiveJwtDecoder;

    @Autowired
    private ApplicationContext context;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        setupServiceMocks();

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

    private void setupServiceMocks() {
        UserProfileResponseDto mockDtoUser = new UserProfileResponseDto(
                "user1",
                "User One",
                LocalDate.now(),
                List.of());

        AccountFullResponseDto mockDto = new AccountFullResponseDto(
                "user1", "User One", LocalDate.now(), UUID.randomUUID(),
                BigDecimal.valueOf(500), Currency.RUB, 1L);

        when(userService.getOrCreateUser(any()))
                .thenReturn(Mono.just(mockDtoUser));

        when(accountService.getOtherAccounts(anyString()))
                .thenReturn(Flux.just(new AccountShortDto("user2", "User Two", Currency.RUB)));

        when(accountService.changeBalance(anyString(), any(), anyString()))
                .thenReturn(Mono.just(OperationResultDto.<Void>builder().success(true).message("Баланс обновлен").build()));

        when(userService.updateProfile(any(), any()))
                .thenReturn(Mono.just(mockDto));
    }
}
