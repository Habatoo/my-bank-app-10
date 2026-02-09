package io.github.habatoo.services;

import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Сервис управления данными профиля пользователя.
 * <p>
 * Обеспечивает логику просмотра и редактирования персональных данных клиента.
 * Функциональный блок в интерфейсе включает:
 * <ul>
 * <li>Редактируемые поля: "Фамилия Имя" и "Дата рождения".</li>
 * <li>Информационное поле: "Текущая сумма на счете" (только для чтения).</li>
 * <li>Кнопку «Обновить профиль» для фиксации изменений.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Правила валидации:</b>
 * 1. Обязательное заполнение всех полей формы.
 * 2. Возраст пользователя должен быть не менее 18 лет.
 * В случае нарушения правил изменения не сохраняются, и пользователю выводится ошибка.
 * </p>
 */
public interface UserFrontService {

    /**
     * Выполняет обновление персональных данных аккаунта.
     * <p>
     * Метод извлекает данные формы из контекста текущего запроса {@link ServerWebExchange},
     * проводит проверку бизнес-правил (полнота данных, возрастное ограничение)
     * и сохраняет изменения через микросервис пользователей.
     * </p>
     *
     * @param exchange контекст текущего серверного запроса, содержащий данные формы.
     * @return {@link Mono} с объектом {@link RedirectView}, выполняющим перенаправление
     * на главную страницу после завершения операции с передачей статуса (успех/ошибка).
     */
    Mono<RedirectView> updateProfile(ServerWebExchange exchange);

    Mono<RedirectView> updatePassword(ServerWebExchange exchange);

    Mono<RedirectView> openNewAccount(ServerWebExchange exchange);
}
