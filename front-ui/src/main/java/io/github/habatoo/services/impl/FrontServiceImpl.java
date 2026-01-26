package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.services.FrontService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Collections;
import java.util.List;

import static io.github.habatoo.constants.ApiConstants.BASE_GATEWAY_URL;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrontServiceImpl implements FrontService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry registry;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Rendering> showMainPage(String info, String error) {
        return fetchAccountData()
                .zipWith(fetchAllAccounts())
                .map(tuple -> {
                    AccountFullResponseDto account = tuple.getT1();

                    return getRendering(info, error, tuple, account);
                });
    }

    private Rendering getRendering(
            String info,
            String error,
            Tuple2<AccountFullResponseDto, List<AccountFullResponseDto>> tuple,
            AccountFullResponseDto account) {
        return Rendering.view("main")
                .modelAttribute("account", account)
                .modelAttribute("name", account.getName())
                .modelAttribute("birthdate", account.getBirthDate())
                .modelAttribute("sum", account.getBalance())
                .modelAttribute("accounts", tuple.getT2())
                .modelAttribute("info", info)
                .modelAttribute("errors", error != null ? List.of(error) : null)
                .build();
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
                })
                .switchIfEmpty(Mono.error(new RuntimeException("User data not found")));
    }

    private Mono<List<AccountFullResponseDto>> fetchAllAccounts() {
        CircuitBreaker cb = registry.circuitBreaker("gateway-cb");

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> webClient.get()
                        .uri(BASE_GATEWAY_URL + "/main/users")
                        .retrieve()
                        .bodyToFlux(AccountFullResponseDto.class)
                        .transformDeferred(CircuitBreakerOperator.of(cb))
                )
                .collectList()
                .onErrorReturn(Collections.emptyList())
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }
}
