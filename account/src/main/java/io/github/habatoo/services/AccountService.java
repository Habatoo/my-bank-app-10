package io.github.habatoo.services;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountService {
    Mono<AccountFullResponseDto> getByLogin(String login);

    Flux<AccountShortDto> getOtherAccounts(String currentLogin);

    Mono<Void> changeBalance(String login, BigDecimal delta);
}
