package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит-тестирование механизмов автоконфигурации безопасности {@link SecurityAutoConfiguration}.
 * <p>
 * Класс использует {@link ReactiveWebApplicationContextRunner} для проверки того, что при определенных условиях
 * в контексте приложения создаются необходимые бины безопасности. Это позволяет убедиться, что аннотации
 * автоконфигурации и зависимости между компонентами настроены верно.
 * </p>
 */
@DisplayName("Юнит-тесты для SecurityAutoConfiguration")
class SecurityAutoConfigurationTest {

    /**
     * Статический конфигурационный класс для имитации необходимых зависимостей безопасности.
     * <p>
     * Предоставляет моки для инфраструктуры OAuth2 и JWT, которые требуются для работы
     * {@link ServerHttpSecurity} в неблокирующем окружении.
     * </p>
     */
    @Configuration
    static class MockSecurityDependenciesConfiguration {

        /**
         * Создает мок репозитория регистраций клиентов.
         *
         * @return мок {@link ReactiveClientRegistrationRepository}.
         */
        @Bean
        public ReactiveClientRegistrationRepository clientRegistrationRepository() {
            return mock(ReactiveClientRegistrationRepository.class);
        }

        /**
         * Создает мок декодера JWT токенов.
         *
         * @return мок {@link ReactiveJwtDecoder}.
         */
        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return mock(ReactiveJwtDecoder.class);
        }
    }

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
            .withUserConfiguration(MockSecurityDependenciesConfiguration.class);

    /**
     * Тест проверяет, что основной бин цепочки фильтров безопасности {@link SecurityWebFilterChain}
     * успешно создается и регистрируется в контексте под стандартным именем.
     * <p>
     * Это гарантирует, что конфигурация {@link SecurityAutoConfiguration} работоспособна
     * при наличии всех требуемых зависимостей.
     * </p>
     */
    @Test
    @DisplayName("Должен регистрировать основные бины безопасности")
    void shouldRegisterSecurityBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
            assertThat(context).hasBean("securityWebFilterChain");
        });
    }
}
