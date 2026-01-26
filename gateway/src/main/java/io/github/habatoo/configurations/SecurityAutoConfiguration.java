package io.github.habatoo.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Основной класс конфигурации безопасности для реактивного микросервиса.
 * <p>
 * Данная конфигурация активирует поддержку WebFlux Security и настраивает правила
 * фильтрации запросов, обработку исключений и интеграцию с OAuth2 (Resource Server и Client).
 * </p>
 *
 * @see EnableWebFluxSecurity
 * @see SecurityWebFilterChain
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityAutoConfiguration {

    /**
     * Настраивает цепочку фильтров безопасности (Security Filter Chain).
     * * <p>Основные настройки включают:</p>
     * <ul>
     * <li>Отключение CSRF (Cross-Site Request Forgery), так как сервис является Stateless API.</li>
     * <li>Разрешение анонимного доступа к эндпоинтам мониторинга (Actuator).</li>
     * <li>Требование обязательной аутентификации для всех остальных запросов.</li>
     * <li>Кастомная обработка ошибок аутентификации (возврат 401 Unauthorized вместо редиректа).</li>
     * <li>Поддержка JWT в качестве механизма аутентификации (OAuth2 Resource Server).</li>
     * <li>Активация возможностей OAuth2 Client для взаимодействия с внешними сервисами.</li>
     * </ul>
     *
     * @param http объект для построения правил безопасности на уровне HTTP-запросов.
     * @return настроенная цепочка фильтров безопасности {@link SecurityWebFilterChain}.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .oauth2Client(withDefaults())
                .build();
    }
}
