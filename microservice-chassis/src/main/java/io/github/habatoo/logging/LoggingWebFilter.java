package io.github.habatoo.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class LoggingWebFilter implements WebFilter {
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
