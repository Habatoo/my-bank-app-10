package io.github.habatoo.controllers;

import io.github.habatoo.FrontApplication;
import io.github.habatoo.configurations.TestSecurityConfiguration;
import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.services.CashFrontService;
import io.github.habatoo.services.FrontService;
import io.github.habatoo.services.TransferFrontService;
import io.github.habatoo.services.UserFrontService;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                FrontApplication.class,
                TestSecurityConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMessageVerifier
public abstract class BaseContractTest {

    @MockitoBean
    protected FrontService frontService;

    @MockitoBean
    protected CashFrontService cashFrontService;

    @MockitoBean
    protected TransferFrontService transferFrontService;

    @MockitoBean
    protected UserFrontService userFrontService;

    @Autowired
    private ApplicationContext context;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        when(cashFrontService.moveMoney(any(CashDto.class)))
                .thenReturn(Mono.just("redirect:/main?info=success"));
        when(transferFrontService.sendMoney(any(TransferDto.class)))
                .thenReturn(Mono.just("redirect:/main?info=transfer_ok"));
        when(userFrontService.updateProfile(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(new RedirectView("/main")));
        when(frontService.showMainPage(any(), any()))
                .thenReturn(Mono.just(Rendering.view("main")
                        .modelAttribute("info", "Операция выполнена")
                        .modelAttribute("error", "Произошла ошибка")
                        .build()));

        webTestClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(mockJwt()
                        .jwt(builder -> builder
                                .subject("367c3cf3-af3e-44af-ab7b-6d2034c6fca6")
                                .claim("preferred_username", "user1")
                        ));

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
