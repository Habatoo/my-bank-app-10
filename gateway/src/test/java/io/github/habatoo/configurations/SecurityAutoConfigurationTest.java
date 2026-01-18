package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит тесты бина SecurityAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для SecurityAutoConfiguration")
class SecurityAutoConfigurationTest {

    @Configuration
    static class MockSecurityDependenciesConfiguration {
        @Bean
        public ReactiveClientRegistrationRepository clientRegistrationRepository() {
            return mock(ReactiveClientRegistrationRepository.class);
        }

        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return mock(ReactiveJwtDecoder.class);
        }
    }

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
            .withUserConfiguration(MockSecurityDependenciesConfiguration.class);

    @Test
    @DisplayName("Должен регистрировать основные бины безопасности")
    void shouldRegisterSecurityBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
            assertThat(context).hasBean("securityWebFilterChain");
        });
    }
}
