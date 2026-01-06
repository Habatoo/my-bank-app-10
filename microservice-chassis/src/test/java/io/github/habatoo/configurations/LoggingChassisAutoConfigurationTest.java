package io.github.habatoo.configurations;

import io.github.habatoo.logging.LoggingWebFilter;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит тесты бина JsonChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для LoggingChassisAutoConfiguration")
class LoggingChassisAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoggingChassisAutoConfiguration.class));

    @Test
    @DisplayName("Должен успешно создавать бины логирования и фильтра")
    void shouldCreateLoggingBeansTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LoggingWebFilter.class);
            assertThat(context).hasSingleBean(ObservationRegistry.class);
        });
    }

    /**
     * Проверяет отсутствие бина, если конфигурация не была загружена.
     */
    @Test
    @DisplayName("Контекст не должен содержать обработчик, если конфигурация не подключена")
    void shouldNotContainHandlerWithoutConfigTest() {
        new ApplicationContextRunner()
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingWebFilter.class);
                    assertThat(context).doesNotHaveBean(ObservationRegistry.class);
                });
    }
}
