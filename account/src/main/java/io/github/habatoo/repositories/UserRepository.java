package io.github.habatoo.repositories;

import io.github.habatoo.models.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link User}.
 * <p>
 * Предоставляет реактивные методы доступа к данным пользователей в базе данных
 * через Spring Data R2DBC. Обеспечивает выполнение CRUD-операций и
 * специализированных поисковых запросов без блокировки потоков.
 * </p>
 */
public interface UserRepository extends R2dbcRepository<User, UUID> {

    /**
     * Выполняет поиск пользователя по его уникальному логину.
     * <p>
     * Метод используется в процессах аутентификации и сопоставления
     * локальных данных с данными из внешних провайдеров (Identity Providers).
     * </p>
     *
     * @param login строковый идентификатор пользователя (username).
     * @return {@link Mono} с найденным пользователем или пустой результат.
     */
    Mono<User> findByLogin(String login);

    /**
     * Проверяет существование пользователя с заданным логином.
     * <p>
     * Удобно для валидации уникальности данных при регистрации новых
     * пользователей или проверке прав доступа.
     * </p>
     *
     * @param login строковый идентификатор пользователя.
     * @return {@link Mono}, содержащий {@code true}, если логин занят, иначе {@code false}.
     */
    Mono<Boolean> existsByLogin(String login);

    /**
     * Получает список всех пользователей, исключая пользователя с указанным логином.
     * <p>
     * Метод часто применяется для формирования списка выбора получателей перевода,
     * чтобы исключить отправителя из списка возможных адресатов.
     * </p>
     *
     * @param login логин пользователя, которого необходимо исключить из выборки.
     * @return {@link Flux} (поток) пользователей, доступных для взаимодействия.
     */
    Flux<User> findAllByLoginNot(String login);
}
