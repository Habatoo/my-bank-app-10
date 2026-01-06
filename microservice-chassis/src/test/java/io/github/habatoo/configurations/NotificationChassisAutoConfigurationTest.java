package io.github.habatoo.configurations;

import io.github.habatoo.services.NotificationClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит тесты бина NotificationChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для NotificationChassisAutoConfiguration")
class NotificationChassisAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationChassisAutoConfiguration.class))
            .withBean(WebClientChassisAutoConfiguration.class, () -> mock(WebClientChassisAutoConfiguration.class))
            .withBean(ReactiveClientRegistrationRepository.class, () -> mock(ReactiveClientRegistrationRepository.class))
            .withBean(ServerOAuth2AuthorizedClientRepository.class, () -> mock(ServerOAuth2AuthorizedClientRepository.class));

    /**
     * Тест успешного создания бина.
     */
    @Test
    @DisplayName("Бин автоконфигурации должен успешно создаваться в контексте")
    void shouldCreateBeanTest() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NotificationChassisAutoConfiguration.class);
        });
    }

    /**
     * Проверяет, что NotificationClientService регистрируется как бин.
     */
    @Test
    @DisplayName("Должен регистрировать бин NotificationClientService в контексте")
    void shouldRegisterGlobalNotificationClientServiceTest() {
        contextRunner.run(context -> {
            NotificationClientService handler = context.getBean(NotificationClientService.class);
            assertThat(handler).isNotNull();
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
                    assertThat(context).doesNotHaveBean(NotificationClientService.class);
                });
    }
}