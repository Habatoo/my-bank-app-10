package io.github.habatoo.cash.repositories;

import io.github.habatoo.cash.BaseCashTest;
import io.github.habatoo.dto.enums.OperationType;
import io.github.habatoo.models.Cash;
import io.github.habatoo.repositories.OperationsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link OperationsRepository}.
 * Проверяют корректность взаимодействия с таблицей кассовых операций.
 */
@DisplayName("Тестирование OperationsRepository")
public class OperationsRepositoryIntegrationTest extends BaseCashTest {

    /**
     * Тестирует сохранение новой кассовой операции и её последующий поиск по ID.
     */
    @Test
    @DisplayName("Save & FindById: Успешное сохранение и получение операции")
    void saveAndFindByIdShouldWorkTest() {
        Cash cashOperation = createCash("name", new BigDecimal("500.00"), OperationType.GET);

        var action = operationsRepository.deleteAll()
                .then(operationsRepository.save(cashOperation))
                .then(operationsRepository.findAll().collectList());

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found.getFirst().getUsername()).isEqualTo("name");
                    assertThat(found.getFirst().getAmount()).isEqualByComparingTo("500.00");
                })
                .verifyComplete();
    }

    /**
     * Тестирует поведение при поиске несуществующей записи.
     */
    @Test
    @DisplayName("FindById: Возврат пустого Mono, если операция не найдена")
    void findByIdShouldReturnEmptyMonoWhenNotFoundTest() {
        var action = operationsRepository.deleteAll()
                .then(operationsRepository.findById(UUID.randomUUID()));

        StepVerifier.create(action)
                .verifyComplete();
    }

    /**
     * Тестирует удаление записи из таблицы.
     */
    @Test
    @DisplayName("Delete: Успешное удаление операции")
    void deleteShouldRemoveOperationTest() {
        Cash cash = createCash("name", new BigDecimal("500.00"), OperationType.GET);

        var action = operationsRepository.save(cash)
                .flatMap(saved -> operationsRepository.deleteById(saved.getId()))
                .then(operationsRepository.findAll().collectList());

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found).isEmpty();
                })
                .verifyComplete();
    }
}
