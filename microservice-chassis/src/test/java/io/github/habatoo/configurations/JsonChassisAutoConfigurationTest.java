package io.github.habatoo.configurations;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит тесты бина JsonChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для JsonChassisAutoConfiguration")
class JsonChassisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JsonChassisAutoConfiguration.class,
                    JacksonAutoConfiguration.class
            ));

    /**
     * Тест успешного создания бина.
     */
    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JsonChassisAutoConfiguration.class);
        });
    }

    /**
     * Проверяет, что Jackson2ObjectMapperBuilderCustomizer регистрируется как бин.
     */
    @Test
    @DisplayName("Должен регистрировать бин Jackson2ObjectMapperBuilderCustomizer")
    void shouldRegisterCustomizerTest() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("jsonCustomizer");
            assertThat(context).getBeanNames(Jackson2ObjectMapperBuilderCustomizer.class)
                    .contains("jsonCustomizer");
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
                    assertThat(context).doesNotHaveBean(Jackson2ObjectMapperBuilderCustomizer.class);
                });
    }

    /**
     * Должен настраивать ObjectMapper согласно ТЗ (ISO даты, NON_NULL, игнор неизвестных).
     */
    @Test
    @DisplayName("Тест настройки ObjectMapper согласно ТЗ")
    void shouldApplyCustomSettingsToObjectMapperTest() {
        String customFormat = "yyyy-MM-dd HH:mm:ss";

        contextRunner.withPropertyValues("spring.application.date=" + customFormat)
                .run(context -> {
                    Jackson2ObjectMapperBuilder builder = context.getBean(Jackson2ObjectMapperBuilder.class);
                    ObjectMapper mapper = builder.build();

                    assertThat(mapper.getSerializationConfig().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
                    assertThat(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();

                    String json = mapper.writeValueAsString(new TestObject(null, "active"));
                    assertThat(json).doesNotContain("nullField");

                    LocalDateTime now = LocalDateTime.of(2026, 1, 5, 10, 0);
                    String dateJson = mapper.writeValueAsString(new DateObject(now));
                    assertThat(dateJson).contains("2026-01-05T10:00:00");
                });
    }

    static class TestObject {
        public String nullField;
        public String status;

        public TestObject(String nullField, String status) {
            this.nullField = nullField;
            this.status = status;
        }
    }

    static class DateObject {
        public LocalDateTime dateTime;

        public DateObject(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }
    }
}
