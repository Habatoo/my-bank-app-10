package io.github.habatoo.configurations;

import io.github.habatoo.repositories.OutboxRepository;
import io.github.habatoo.services.NotificationClientService;
import io.github.habatoo.services.OutboxClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит тесты бина ServicesChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для ServicesChassisAutoConfiguration")
class ServicesChassisAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServicesChassisAutoConfiguration.class))
            .withBean("backgroundWebClient", WebClient.class, () -> mock(WebClient.class))
            .withBean(CircuitBreakerRegistry.class, () -> mock(CircuitBreakerRegistry.class))
            .withBean(OutboxRepository.class, () -> mock(OutboxRepository.class));

    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ServicesChassisAutoConfiguration.class);
        });
    }

    @Test
    @DisplayName("Должен регистрировать бин NotificationClientService в контексте")
    void shouldRegisterGlobalNotificationClientServiceTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NotificationClientService.class);
            NotificationClientService handler = context.getBean(NotificationClientService.class);
            assertThat(handler).isNotNull();
        });
    }

    @Test
    @DisplayName("Должен регистрировать бин OutboxClientService в контексте")
    void shouldRegisterOutboxClientServiceTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OutboxClientService.class);
        });
    }

    @Test
    @DisplayName("Контекст не должен содержать обработчик, если конфигурация не подключена")
    void shouldNotContainHandlerWithoutConfigTest() {
        new ApplicationContextRunner()
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NotificationClientService.class);
                    assertThat(context).doesNotHaveBean(OutboxClientService.class);
                });
    }
}
