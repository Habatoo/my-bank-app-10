package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Юнит тесты бина WebClientChassisAutoConfiguration и связанных бинов.
 */
@DisplayName("Юнит-тесты для WebClientChassisAutoConfiguration")
class WebClientChassisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebClientChassisAutoConfiguration.class))
            .withBean(ReactiveClientRegistrationRepository.class, () -> mock(ReactiveClientRegistrationRepository.class))
            .withBean(ServerOAuth2AuthorizedClientRepository.class, () -> mock(ServerOAuth2AuthorizedClientRepository.class));

    @Test
    @DisplayName("Должен регистрировать WebClient.Builder с поддержкой LoadBalancer")
    void shouldRegisterLoadBalancedWebClientBuilder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebClient.Builder.class);

            WebClient.Builder builder = context.getBean(WebClient.Builder.class);
            assertThat(builder).isNotNull();

            String[] beanNames = context.getBeanNamesForAnnotation(LoadBalanced.class);
            assertThat(beanNames).contains("loadBalancedWebClientBuilder");
        });
    }

    @Test
    @DisplayName("Должен создавать бин WebClient")
    void shouldRegisterWebClient() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebClient.class);

            WebClient webClient = context.getBean(WebClient.class);
            assertThat(webClient).isNotNull();
        });
    }

    @Test
    @DisplayName("Контекст не должен падать, если WebClient уже определен пользователем")
    void shouldNotConflictWithCustomWebClient() {
        contextRunner
                .withBean("customWebClient", WebClient.class, () -> WebClient.builder().build())
                .run(context -> {
                    assertThat(context).getBeanNames(WebClient.class).hasSize(2);
                });
    }
}
