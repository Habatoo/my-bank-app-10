package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит тесты бина SecurityChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для SecurityChassisAutoConfiguration")
class SecurityChassisAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityChassisAutoConfiguration.class))
            .withBean(ReactiveJwtDecoder.class, () -> mock(ReactiveJwtDecoder.class));

    @Test
    @DisplayName("Должен регистрировать основные бины безопасности")
    void shouldRegisterSecurityBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("jwtAuthenticationConverter");
            assertThat(context).hasSingleBean(ReactiveJwtAuthenticationConverter.class);
            assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
        });
    }

    @Test
    @DisplayName("Конвертер должен успешно извлекать роли из realm_access и добавлять префикс ROLE_")
    void jwtAuthenticationConverterShouldMapRolesCorrectly() {
        contextRunner.run(context -> {
            ReactiveJwtAuthenticationConverter converter = context.getBean(ReactiveJwtAuthenticationConverter.class);

            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .claim("realm_access", Map.of("roles", List.of("admin", "user")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(auth -> {
                        assertThat(auth.getAuthorities()).containsExactlyInAnyOrder(
                                new SimpleGrantedAuthority("ROLE_admin"),
                                new SimpleGrantedAuthority("ROLE_user")
                        );
                    })
                    .verifyComplete();
        });
    }

    @Test
    @DisplayName("Конвертер не должен падать, если секция realm_access отсутствует")
    void jwtAuthenticationConverterShouldHandleMissingClaims() {
        contextRunner.run(context -> {
            ReactiveJwtAuthenticationConverter converter = context.getBean(ReactiveJwtAuthenticationConverter.class);

            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .build();

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(auth -> assertThat(auth.getAuthorities()).isEmpty())
                    .verifyComplete();
        });
    }
}
