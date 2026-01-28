package io.github.habatoo.configurations;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@TestConfiguration
@Profile("test")
@EnableReactiveMethodSecurity
public class TestSecurityConfiguration {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                )
                .build();
    }

    /**
     * Создаем заглушку декодера.
     * Теперь любой токен в заголовке "Authorization: Bearer ..."
     * будет превращаться в этот объект Jwt.
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> Mono.just(Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("preferred_username", "user1")
                .claim("groups", List.of("ROLE_USER", "ROLE_ADMIN"))
                .subject("367c3cf3-af3e-44af-ab7b-6d2034c6fca6")
                .claim("sub", UUID.fromString("367c3cf3-af3e-44af-ab7b-6d2034c6fca6"))
                .build());
    }

    /**
     * Конвертер, который достает роли из клеймов JWT и превращает их в Authorities.
     */
    private ReactiveJwtAuthenticationConverterAdapter grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("groups");
            return groups.stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(it -> (org.springframework.security.core.GrantedAuthority) it)
                    .toList();
        });

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
