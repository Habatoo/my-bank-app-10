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

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient webClient(WebClient.Builder loadBalancedWebClientBuilder,
                               ReactiveClientRegistrationRepository clientRegistrations,
                               ServerOAuth2AuthorizedClientRepository authorizedClients) {

        return loadBalancedWebClientBuilder
                .filter(createOauthFilter(clientRegistrations, authorizedClients))
                .build();
    }

    @Bean
    public WebClient simpleWebClient(ReactiveClientRegistrationRepository clientRegistrations,
                                     ServerOAuth2AuthorizedClientRepository authorizedClients) {
        return WebClient.builder()
                .filter(createOauthFilter(clientRegistrations, authorizedClients))
                .build();
    }

    private ServerOAuth2AuthorizedClientExchangeFilterFunction createOauthFilter(
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {

        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultClientRegistrationId("keycloak");

        oauth.setDefaultOAuth2AuthorizedClient(true);

        return oauth;
    }
}
