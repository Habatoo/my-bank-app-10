package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final WebClient webClient;

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    private static final String BASE_URL = "http://gateway/api";

    @GetMapping("/")
    public Mono<RedirectView> getMainPage() {
        return Mono.just(new RedirectView("/main"));
    }

    @GetMapping("/main")
    public Mono<Rendering> getAccount(@AuthenticationPrincipal OAuth2User principal) {
        return Mono.zip(fetchAccountData(), fetchAllAccounts())
                .map(tuple -> fillModel(tuple.getT1(), tuple.getT2()))
                .onErrorResume(e -> {
                    log.error("Failed to fetch data", e);
                    return Mono.just(Rendering.view("main")
                            .modelAttribute("errors", List.of("Ошибка данных: " + e.getMessage()))
                            .build());
                });
    }

    private Mono<AccountDto> fetchAccountData() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String externalId;
                    if (auth.getPrincipal() instanceof Jwt jwt) {
                        externalId = jwt.getSubject();
                        log.info("API CALL: Подготовка запроса для externalId (JWT) {}", externalId);
                    } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
                        externalId = oidcUser.getSubject();
                        log.info("API CALL: Подготовка запроса для externalId (OIDC) {}", externalId);
                    } else {
                        externalId = "UNKNOWN";
                        log.info("API CALL: Подготовка запроса для пользователя {}", auth.getName());
                    }

                    log.info("Sending GET request to: {}/main/user", BASE_URL);

                    return webClient.get()
                            .uri(BASE_URL + "/main/user")
                            .exchangeToMono(response -> {
                                log.info("Gateway ответ со status code: {}", response.statusCode());
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(AccountDto.class);
                                } else {
                                    return response.createException().flatMap(Mono::error);
                                }
                            })
                            .doOnNext(dto -> log.info("Данные успешно получены для: {}", externalId));
                })
                .doOnError(e -> log.error("ОШИБКА при вызове Гейтвея: {}", e.getMessage()));
    }

    private Mono<List<AccountDto>> fetchAllAccounts() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> {
                    log.info("Sending Flux GET request to: {}/main/user", BASE_URL);
                    return webClient.get()
                            .uri(BASE_URL + "/main/user")
                            .retrieve()
                            .bodyToFlux(AccountDto.class);
                })
                .collectList()
                .doOnError(e -> log.error("ОШИБКА fetchAllAccounts: {}", e.getMessage()))
                .onErrorReturn(Collections.emptyList());
    }

    private Rendering fillModel(
            AccountDto currentAccount,
            List<AccountDto> otherAccounts) {
        return Rendering.view("main")
                .modelAttribute("name", currentAccount.getName() != null ? currentAccount.getName() : "Имя не задано")
                .modelAttribute("birthdate", currentAccount.getBirthdate() != null ? currentAccount.getBirthdate() : LocalDate.now())
                .modelAttribute("sum", currentAccount.getBalance())
                .modelAttribute("accounts", otherAccounts)
                .build();
    }
}
