package io.github.habatoo.cofigurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Конфигурация безопасности приложения.
 * <p>
 * Настраивает цепочку фильтров безопасности для реактивного окружения (WebFlux).
 * Обеспечивает защиту ресурсов, аутентификацию через OAuth2 (OpenID Connect)
 * и управление сеансами выхода (logout).
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityAutoConfiguration {

    /**
     * Определяет основные правила безопасности (SecurityWebFilterChain).
     *
     * @param http                         объект для настройки безопасности HTTP-запросов.
     * @param clientRegistrationRepository репозиторий регистраций клиентов OAuth2/OIDC.
     * @return настроенная цепочка фильтров безопасности.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/**", "/login/**", "/error", "/css/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                )
                .build();
    }

    /**
     * Настраивает обработчик успешного выхода из системы для протокола OpenID Connect (OIDC).
     * <p>
     * Позволяет корректно завершить сессию локально и на стороне Keycloak.
     * </p>
     * * @param repository репозиторий конфигураций OAuth2 клиентов.
     *
     * @return обработчик завершения сессии (LogoutSuccessHandler).
     */
    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {
        var logoutHandler = new OidcClientInitiatedServerLogoutSuccessHandler(repository);
        logoutHandler.setPostLogoutRedirectUri("{baseUrl}");

        return logoutHandler;
    }
}
