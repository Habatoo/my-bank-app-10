package io.github.habatoo.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для ResilienceProperties — проверяют загрузку пропертей и их значения.
 */
@DisplayName("Тест загрузки ResilienceProperties")
class ResiliencePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ResiliencePropertiesTest.TestConfig.class)
            .withPropertyValues(
                    "spring.application.name=test-service",
                    "spring.resilience.sliding-window-size=10",
                    "spring.resilience.failure-rate-threshold=50",
                    "spring.resilience.wait-duration-in-open-state=10",
                    "spring.resilience.permitted-number-of-calls-in-half-open-state=3"
            );

    /**
     * Проверяет, что конфигурационные параметры Resilience корретно подгружаются
     * и бин {@link ResilienceProperties} связывается из источника настроек.
     */
    @Test
    @DisplayName("Тест загрузки ResilienceProperties из YAML")
    void shouldLoadPropertiesFromYamlTest() {
        contextRunner.run(context -> {
            var props = context.getBean(ResilienceProperties.class);
            assertThat(props.instanceName()).isEqualTo(null);
            assertThat(props.slidingWindowSize()).isEqualTo(10);
            assertThat(props.failureRateThreshold()).isEqualTo(50);
            assertThat(props.waitDurationInOpenState()).isEqualTo(10);
            assertThat(props.permittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        });
    }

    @EnableConfigurationProperties(ResilienceProperties.class)
    static class TestConfig {
    }
}
