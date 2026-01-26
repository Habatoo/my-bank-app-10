package io.github.habatoo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Резервный контроллер для обработки отказов микросервисов (Fallback Mechanism).
 * <p>
 * Данный контроллер предоставляет эндпоинты, на которые перенаправляется трафик
 * в случае обнаружения разрыва цепи (Circuit Breaker open state) или превышения
 * времени ожидания (timeout) при вызове смежных сервисов.
 * </p>
 *
 * @see <a href="https://resilience4j.readme.io/docs/circuitbreaker">Resilience4j Circuit Breaker</a>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Возвращает резервное сообщение при недоступности сервиса кассовых операций (Cash Service).
     * <p>
     * Вызывается автоматически шлюзом (Gateway) или клиентом, если сервис пополнения/снятия
     * не отвечает в течение установленного времени.
     * </p>
     *
     * @return {@link Mono} с ответом, содержащим статус 503 (Service Unavailable) и описание ошибки.
     */
    @GetMapping("/cash-unavailable")
    public Mono<ResponseEntity<String>> cashFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис кассовых операций временно недоступен. Попробуйте позже."));
    }

    /**
     * Возвращает резервное сообщение при недоступности сервиса управления счетами (Account Service).
     * <p>
     * Предотвращает каскадный отказ системы, если операции по запросу данных счета
     * не могут быть выполнены из-за проблем с сетью или базой данных сервиса Account.
     * </p>
     *
     * @return {@link Mono} с ответом, содержащим статус 503 (Service Unavailable) и описание ошибки.
     */
    @GetMapping("/account-unavailable")
    public Mono<ResponseEntity<String>> accountFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис по работе со счетом временно недоступен. Попробуйте позже."));
    }

    /**
     * Возвращает резервное сообщение при недоступности сервиса межбанковских переводов (Transfer Service).
     * <p>
     * Используется для информирования пользователя о временной невозможности проведения
     * транзакции между аккаунтами.
     * </p>
     *
     * @return {@link Mono} с ответом, содержащим статус 503 (Service Unavailable) и описание ошибки.
     */
    @GetMapping("/transfer-unavailable")
    public Mono<ResponseEntity<String>> transferFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Сервис переводов временно недоступен. Попробуйте позже."));
    }
}
