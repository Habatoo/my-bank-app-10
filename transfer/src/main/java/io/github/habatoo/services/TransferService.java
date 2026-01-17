package io.github.habatoo.services;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import reactor.core.publisher.Mono;

public interface TransferService {
    Mono<OperationResultDto<TransferDto>> processTransferOperation(String senderLogin, TransferDto transferDto);
}
