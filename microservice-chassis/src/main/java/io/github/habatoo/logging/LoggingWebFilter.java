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

    public static Mono<ServerWebExchange> getExchange() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(ServerWebExchange.class)) {
                return Mono.just(ctx.get(ServerWebExchange.class));
            }
            return Mono.empty();
        });
    }

    /**
     * Фильтр для входЯщих запросов - игнорирует эндпоинты health-check,
     * Проводит замеры времени исполнения запроса.
     *
     * @param exchange the current server exchange
     * @param chain    цепочка делегирования фильтров
     * @return Void
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.contains("/actuator/health")) {
            return chain.filter(exchange);
        }

        long start = System.currentTimeMillis();
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange))
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - start;
                    log.info("API CALL: {} {} | STATUS: {} | TIME: {}ms",
                            exchange.getRequest().getMethod(), path,
                            exchange.getResponse().getStatusCode(), duration);
                });
    }
}
