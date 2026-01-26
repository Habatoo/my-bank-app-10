package io.github.habatoo.configurations;

import io.github.habatoo.handlers.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит тесты бина ExceptionChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для ExceptionChassisAutoConfiguration")
class ExceptionChassisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExceptionChassisAutoConfiguration.class));

    /**
     * Тест успешного создания бина.
     */
    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExceptionChassisAutoConfiguration.class);
        });
    }

    /**
     * Проверяет, что GlobalExceptionHandler регистрируется как бин.
     */
    @Test
    @DisplayName("Должен регистрировать бин GlobalExceptionHandler в контексте")
    void shouldRegisterGlobalExceptionHandlerTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);

            GlobalExceptionHandler handler = context.getBean(GlobalExceptionHandler.class);
            assertThat(handler).isNotNull();
        });
    }

    /**
     * Проверяет отсутствие бина, если конфигурация не была загружена.
     */
    @Test
    @DisplayName("Контекст не должен содержать обработчик, если конфигурация не подключена")
    void shouldNotContainHandlerWithoutConfig() {
        new ApplicationContextRunner()
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
                });
    }
}