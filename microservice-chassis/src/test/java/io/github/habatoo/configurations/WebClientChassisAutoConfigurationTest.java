package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты для конфигурации WebClientChassisAutoConfiguration.
 */
@DisplayName("Юнит-тесты для WebClientChassisAutoConfiguration")
class WebClientChassisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebClientChassisAutoConfiguration.class))
            .withBean(ReactiveClientRegistrationRepository.class, () -> mock(ReactiveClientRegistrationRepository.class))
            .withBean(ServerOAuth2AuthorizedClientRepository.class, () -> mock(ServerOAuth2AuthorizedClientRepository.class))
            .withBean(ReactiveOAuth2AuthorizedClientService.class, () -> mock(ReactiveOAuth2AuthorizedClientService.class));

    @Test
    @DisplayName("Должен регистрировать WebClient.Builder с поддержкой LoadBalancer")
    void shouldRegisterLoadBalancedWebClientBuilder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebClient.Builder.class);

            String[] beanNames = context.getBeanNamesForAnnotation(LoadBalanced.class);
            assertThat(beanNames).contains("loadBalancedWebClientBuilder");
        });
    }

    @Test
    @DisplayName("Должен создавать все три бина WebClient")
    void shouldRegisterWebClients() {
        contextRunner.run(context -> {
            assertThat(context).getBeans(WebClient.class).hasSize(3);

            assertThat(context).hasBean("webClient");
            assertThat(context).hasBean("simpleWebClient");
            assertThat(context).hasBean("backgroundWebClient");
        });
    }

    @Test
    @DisplayName("Контекст не должен падать, если WebClient уже определен пользователем")
    void shouldNotConflictWithCustomWebClient() {
        contextRunner
                .withBean("customWebClient", WebClient.class, () -> WebClient.builder().build())
                .run(context -> {
                    assertThat(context).getBeans(WebClient.class).hasSize(4);
                    assertThat(context).hasBean("customWebClient");
                });
    }
}
