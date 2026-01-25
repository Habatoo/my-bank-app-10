package io.github.habatoo.repositories;

import io.github.habatoo.BaseTransferTest;
import io.github.habatoo.models.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link TransfersRepository}.
 * Проверяют корректность выполнения CRUD операций с использованием Testcontainers.
 */
@DisplayName("Тестирование TransfersRepository")
public class TransfersRepositoryIntegrationTest extends BaseTransferTest {

    @Test
    @DisplayName("Save & FindById: Успешное сохранение и получение перевода")
    void saveAndFindByIdShouldWorkTest() {
        Transfer transfer = createTransfer("sender_user", "receiver_user", new BigDecimal("100.00"));

        var action = clearDatabase()
                .then(transfersRepository.save(transfer))
                .flatMap(saved -> transfersRepository.findById(saved.getId()));

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found.getId()).isNotNull();
                    assertThat(found.getSenderUsername()).isEqualTo("sender_user");
                    assertThat(found.getTargetUsername()).isEqualTo("receiver_user");
                    assertThat(found.getAmount()).isEqualByComparingTo("100.00");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("FindById: Возврат пустого Mono, если перевод не найден")
    void findByIdShouldReturnEmptyWhenNotFoundTest() {
        var action = clearDatabase()
                .then(transfersRepository.findById(UUID.randomUUID()));

        StepVerifier.create(action)
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete: Успешное удаление записи о переводе")
    void deleteShouldWorkTest() {
        Transfer transfer = createTransfer("user1", "user2", new BigDecimal("50.00"));

        var action = clearDatabase()
                .then(transfersRepository.save(transfer))
                .flatMap(saved -> transfersRepository.deleteById(saved.getId()))
                .then(transfersRepository.findAll().collectList());

        StepVerifier.create(action)
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Update: Успешное обновление суммы перевода")
    void updateAmountTest() {
        Transfer transfer = createTransfer("user1", "user2", new BigDecimal("10.00"));
        BigDecimal newAmount = new BigDecimal("20.00");

        var action = clearDatabase()
                .then(transfersRepository.save(transfer))
                .map(t -> {
                    t.setAmount(newAmount);
                    return t;
                })
                .flatMap(transfersRepository::save)
                .flatMap(saved -> transfersRepository.findById(saved.getId()));

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found.getAmount()).isEqualByComparingTo(newAmount);
                })
                .verifyComplete();
    }
}