package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    private static final String BASE_URL = "http://gateway:8090/api";

    @GetMapping("/")
    public Mono<RedirectView> getMainPage() {
        return Mono.just(new RedirectView("/main"));
    }

    @GetMapping("/login")
    public Mono<String> getLoginPage() {
        return Mono.just("login");
    }

    @GetMapping("/main")
    public Mono<Rendering> getAccount(Model model) {
        return Mono.zip(fetchAccountData(), fetchAllAccounts())
                .map(tuple -> {
                    AccountDto currentAccount = tuple.getT1();
                    List<AccountDto> otherAccounts = tuple.getT2();

                    return fillModel(currentAccount, otherAccounts);
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch data", e);
                    return Mono.just(Rendering.view("main")
                            .modelAttribute("errors", List.of("Ошибка данных: " + e.getMessage()))
                            .build());
                });
    }

    private Mono<AccountDto> fetchAccountData() {
        return webClient.get()
                .uri(BASE_URL + "/main/user")
                .retrieve()
                .bodyToMono(AccountDto.class)
                .doOnNext(accountDto -> log.info("Fetching accountDto for user: {}", accountDto));
    }

    private Mono<List<AccountDto>> fetchAllAccounts() {
        return webClient.get()
                .uri(BASE_URL + "/main/user")
                .retrieve()
                .bodyToFlux(AccountDto.class)
                .collectList()
                .doOnNext(accountDtoList -> log.info("Fetching List accountDto for user: {}", accountDtoList))
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
