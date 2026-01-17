package io.github.habatoo.services.impl;

import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.TransferDto;
import io.github.habatoo.models.Transfer;
import io.github.habatoo.repositories.TransfersRepository;
import io.github.habatoo.services.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final WebClient webClient;
    private final TransfersRepository transfersRepository;

    @Override
    public Mono<OperationResultDto<TransferDto>> processTransferOperation(
            String senderLogin,
            TransferDto transferDto) {
        BigDecimal amount = transferDto.getValue();
        String recipientLogin = transferDto.getLogin();

        return callAccountService(senderLogin, amount.negate())
                .flatMap(withdrawRes -> {
                    if (!withdrawRes.isSuccess()) {
                        return Mono.just(OperationResultDto.<TransferDto>builder()
                                .success(false)
                                .message("Ошибка списания: " + withdrawRes.getMessage())
                                .build());
                    }

                    return callAccountService(recipientLogin, amount)
                            .flatMap(depositRes -> {
                                if (!depositRes.isSuccess()) {
                                    log.error(
                                            "КРИТИЧЕСКАЯ ОШИБКА: Списано у {}, но не зачислено {}",
                                            senderLogin,
                                            recipientLogin);

                                    // TODO вызов компенсации
                                    return Mono.just(OperationResultDto.<TransferDto>builder()
                                            .success(false)
                                            .message("Не удалось зачислить средства получателю")
                                            .build());
                                }

                                return saveTransferRecord(senderLogin, recipientLogin, amount, transferDto);
                            });
                });
    }

    private Mono<OperationResultDto<Void>> callAccountService(
            String login,
            BigDecimal amount) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host("gateway")
                        .path("/api/account/balance")
                        .queryParam("login", login)
                        .queryParam("amount", amount)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OperationResultDto<Void>>() {
                });
    }

    private Mono<OperationResultDto<TransferDto>> saveTransferRecord(
            String sender,
            String target,
            BigDecimal amount,
            TransferDto dto) {
        Transfer transfer = Transfer.builder()
                .senderUsername(sender)
                .targetUsername(target)
                .amount(amount)
                .createdAt(OffsetDateTime.now())
                .build();

        return transfersRepository.save(transfer)
                .thenReturn(OperationResultDto.<TransferDto>builder()
                        .success(true)
                        .data(dto)
                        .message("Перевод успешно выполнен")
                        .build());
    }
}
