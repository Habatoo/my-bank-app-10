package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит тесты бина Consul.
 */
@DisplayName("Юнит-тесты для ConsulChassisAutoConfiguration")
class ConsulChassisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsulChassisAutoConfiguration.class));

    /**
     * Тест успешного создания бина.
     */
    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConsulChassisAutoConfiguration.class);
        });
    }
}
