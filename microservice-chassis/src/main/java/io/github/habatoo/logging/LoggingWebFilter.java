package io.github.habatoo.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Interceptor (перехватчик) для обеспечения сквозного
 * логирование каждого входящего HTTP-запроса,
 * не вмешиваясь в логику контроллеров.
 */
@Slf4j
public class LoggingWebFilter implements WebFilter {

    /**
     * Фильтр для входЯщих запросов - игнорирует эндпоинты health-check,
     * которые вызываются инфраструктурой Consul. Проводит замеры времени исполнения запроса.
     * @param exchange the current server exchange
     * @param chain цепочка делегирования фильтров
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.contains("/actuator/health")) {
            return chain.filter(exchange);
        }

        long start = System.currentTimeMillis();
        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - start;
                    log.info("API CALL: {} {} | STATUS: {} | TIME: {}ms",
                            exchange.getRequest().getMethod(), path,
                            exchange.getResponse().getStatusCode(), duration);
                });
    }
}
