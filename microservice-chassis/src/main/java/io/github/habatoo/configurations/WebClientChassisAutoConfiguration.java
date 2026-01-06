package io.github.habatoo.configurations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация OAuth2 Client (Для межсервисных вызовов).
 */
@AutoConfiguration
public class WebClientChassisAutoConfiguration {

    /**
     * Балансируемый билдер с возможностью понимать имена сервисов.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Итоговый WebClient.
     */
    @Bean
    public WebClient webClient(WebClient.Builder loadBalancedWebClientBuilder,
                               ReactiveClientRegistrationRepository clientRegistrations,
                               ServerOAuth2AuthorizedClientRepository authorizedClients) {

        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauth.setDefaultClientRegistrationId("keycloak");

        return loadBalancedWebClientBuilder
                .filter(oauth)
                .build();
    }
}
