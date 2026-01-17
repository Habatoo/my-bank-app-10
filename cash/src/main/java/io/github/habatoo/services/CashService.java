package io.github.habatoo.services;

import io.github.habatoo.dto.CashDto;
import reactor.core.publisher.Mono;

public interface CashService {

    Mono<CashDto> processCashOperation(String login, CashDto cashDto);
}
