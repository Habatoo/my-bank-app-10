package io.github.habatoo.configurations;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит тесты бина автологарованя при изменении параметров конфигурации.
 */
@DisplayName("Юнит-тесты для RefreshLoggingAutoConfiguration")
class RefreshLoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshLoggingAutoConfiguration.class));

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setupLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(RefreshLoggingAutoConfiguration.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Тест логирования события при обновлении конфигурации.
     */
    @Test
    @DisplayName("Должен логировать сообщение при наступлении события RefreshScopeRefreshedEvent")
    void shouldLogMessageOnRefreshEventTest() {
        contextRunner.run(context -> {
            String eventName = "custom-refresh-signal";
            RefreshScopeRefreshedEvent event = new RefreshScopeRefreshedEvent(eventName);

            context.publishEvent(event);

            assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains(
                            "=== КОНФИГУРАЦИЯ ОБНОВЛЕНА ===",
                            "Событие (Name): " + eventName,
                            "Источник события: " + eventName
                    );

            assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getLevel)
                    .containsOnly(Level.INFO);
        });
    }

    /**
     * Тест успешного создания бина.
     */
    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RefreshLoggingAutoConfiguration.class);
        });
    }
}
