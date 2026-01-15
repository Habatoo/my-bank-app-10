package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountFullResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final WebClient webClient;
    private static final String BASE_GATEWAY_URL = "http://gateway/api";

    @GetMapping("/")
    public Mono<RedirectView> getMainPage() {
        return Mono.just(new RedirectView("/main"));
    }

    @GetMapping("/main")
    public Mono<Rendering> showMainPage(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String info,
            @RequestParam(required = false) String error) {

        if (principal != null) {
            log.trace("User attributes from Keycloak: {}", principal.getAttributes());

            String birthdate = (String) principal.getAttributes().get("birthdate");
            String initialSum = (String) principal.getAttributes().get("initialSum");

            log.trace("Extracted - Birthdate: {}, InitialSum: {}", birthdate, initialSum);
        }

        return Mono.zip(fetchAccountData(), fetchAllAccounts())
                .map(tuple -> Rendering.view("main")
                        .modelAttribute("account", tuple.getT1())
                        .modelAttribute("name", tuple.getT1().getName())
                        .modelAttribute("birthdate", tuple.getT1().getBirthDate())
                        .modelAttribute("sum", tuple.getT1().getBalance())
                        .modelAttribute("accounts", tuple.getT2())
                        .modelAttribute("info", info)
                        .modelAttribute("errors", error != null ? List.of(error) : null)
                        .build());
    }

    private Mono<AccountFullResponseDto> fetchAccountData() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    log.info("Sending GET request to: {}/main/user", BASE_GATEWAY_URL);
                    return webClient.get()
                            .uri(BASE_GATEWAY_URL + "/main/user")
                            .retrieve()
                            .bodyToMono(AccountFullResponseDto.class);
                });
    }

    private Mono<List<AccountFullResponseDto>> fetchAllAccounts() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> webClient.get()
                        .uri(BASE_GATEWAY_URL + "/main/users")
                        .retrieve()
                        .bodyToFlux(AccountFullResponseDto.class))
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }
}
