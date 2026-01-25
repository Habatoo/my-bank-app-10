package io.github.habatoo.repositories;

import io.github.habatoo.BaseAccountTest;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для проверки корректности работы репозиториев {@link AccountRepository}
 * и {@link UserRepository}.
 * Тестирование проводится с использованием Testcontainers (PostgreSQL) и Liquibase для
 * инициализации схемы базы данных.
 */
@DisplayName("Тестирование AccountRepository и UserRepository с Liquibase миграциями")
class AccountAndUserRepositoryIntegrationTest extends BaseAccountTest {

    /**
     * Тестирует сценарий, при котором поиск счета по несуществующему ID пользователя
     * должен возвращать пустой результат (Empty Mono).
     */
    @Test
    @DisplayName("Поиск аккаунта по userId — Пустой Mono если не найден")
    void findByUserIdShouldReturnEmptyWhenNotExistsTest() {
        var resultMono = clearDatabase()
                .then(accountRepository.findByUserId(UUID.randomUUID()));

        StepVerifier.create(resultMono)
                .verifyComplete();
    }

    /**
     * Тестирует успешный поиск счета по идентификатору пользователя.
     * Проверяет корректность маппинга баланса и связи с таблицей пользователей.
     */
    @Test
    @DisplayName("Account: Поиск по userId — Успех")
    void findByUserIdShouldReturnAccountWhenExistsTest() {
        String login = "ivan_ivanov";
        String balance = "1500.50";
        User user = createUser(login);

        var action = clearDatabase()
                .then(userRepository.save(user))
                .flatMap(savedUser -> {
                    Account account = Account.builder()
                            .id(UUID.randomUUID())
                            .userId(savedUser.getId())
                            .balance(new BigDecimal(balance))
                            .build();
                    return accountRepository.save(account)
                            .thenReturn(savedUser.getId());
                })
                .flatMap(savedUserId -> accountRepository.findByUserId(savedUserId));

        StepVerifier.create(action)
                .assertNext(found -> {
                    assertThat(found.getBalance()).isEqualByComparingTo(balance);
                    assertThat(found.getId()).isNotNull();
                })
                .verifyComplete();
    }

    /**
     * Тестирует поиск пользователя по уникальному логину.
     * Проверяет, что репозиторий корректно извлекает данные при их наличии.
     */
    @Test
    @DisplayName("User: Поиск по логину — Успех")
    void findByLoginShouldReturnUserTest() {
        String login = "marina_sky";
        User user = createUser(login);

        var action = clearDatabase()
                .then(userRepository.save(user))
                .then(userRepository.findByLogin(login));

        StepVerifier.create(action)
                .expectNextMatches(found -> found.getLogin().equals(login))
                .verifyComplete();
    }

    /**
     * Тестирует метод проверки существования логина в базе данных, когда пользователь
     * действительно существует.
     */
    @Test
    @DisplayName("User: Проверка существования логина — Положительный сценарий")
    void existsByLogin_ShouldReturnTrue_WhenUserExists() {
        String login = "active_user";
        User user = createUser(login);

        var action = clearDatabase()
                .then(userRepository.save(user))
                .then(userRepository.existsByLogin(login));

        StepVerifier.create(action)
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * Тестирует метод проверки существования логина, когда запрашиваемый логин
     * отсутствует в базе данных.
     */
    @Test
    @DisplayName("User: Проверка существования логина — Отрицательный сценарий")
    void existsByLogin_ShouldReturnFalse_WhenUserDoesNotExist() {
        var action = clearDatabase()
                .then(userRepository.existsByLogin("unknown_user"));

        StepVerifier.create(action)
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * Тестирует фильтрацию списка пользователей, проверяя исключение конкретного
     * логина (например, отправителя) из общего списка выборки.
     */
    @Test
    @DisplayName("User: Получение всех кроме указанного")
    void findAllByLoginNotShouldExcludeSpecifiedLoginTest() {
        var setup = clearDatabase()
                .then(userRepository.save(createUser("user1")))
                .then(userRepository.save(createUser("user2")))
                .then(userRepository.save(createUser("sender")));

        var resultFlux = setup.thenMany(userRepository.findAllByLoginNot("sender"));

        StepVerifier.create(resultFlux)
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(2)
                .consumeRecordedWith(users -> {
                    assertThat(users).extracting("login")
                            .containsExactlyInAnyOrder("user1", "user2");
                })
                .verifyComplete();
    }
}
