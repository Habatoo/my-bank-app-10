package io.github.habatoo.services;

import io.github.habatoo.dto.CashDto;
import io.github.habatoo.dto.OperationResultDto;
import reactor.core.publisher.Mono;

public interface CashService {
    Mono<OperationResultDto<CashDto>> processCashOperation(String login, CashDto cashDto);
}
