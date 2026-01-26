package io.github.habatoo.configurations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
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

    /**
     * WebClient для обычных HTTP-запросов из контекста WebFlux
     * (есть ServerWebExchange, используется DefaultReactiveOAuth2AuthorizedClientManager).
     */
    @Bean
    public WebClient webClient(WebClient.Builder loadBalancedWebClientBuilder,
                               ReactiveClientRegistrationRepository clientRegistrations,
                               ServerOAuth2AuthorizedClientRepository authorizedClients) {

        return loadBalancedWebClientBuilder
                .filter(createOauthFilter(clientRegistrations, authorizedClients))
                .build();
    }

    /**
     * Простой небалансированный WebClient для обычных HTTP-запросов (тоже из WebFlux-контекста).
     */
    @Bean
    public WebClient simpleWebClient(ReactiveClientRegistrationRepository clientRegistrations,
                                     ServerOAuth2AuthorizedClientRepository authorizedClients) {
        return WebClient.builder()
                .filter(createOauthFilter(clientRegistrations, authorizedClients))
                .build();
    }

    /**
     * Менеджер для фоновых задач (нет ServerWebExchange)
     * — использует AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager backgroundAuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {

        var authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService);

        var authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * WebClient для фоновых задач (@Scheduled, outbox и т.п.), не требующий ServerWebExchange.
     */
    @Bean
    public WebClient backgroundWebClient(
            ReactiveOAuth2AuthorizedClientManager backgroundAuthorizedClientManager) {

        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(backgroundAuthorizedClientManager);
        oauth.setDefaultClientRegistrationId("keycloak");

        return WebClient.builder()
                .filter(oauth)
                .build();
    }

    /**
     * Фильтр OAuth2, завязанный на ServerOAuth2AuthorizedClientRepository
     * (работает только при наличии ServerWebExchange).
     */
    private ServerOAuth2AuthorizedClientExchangeFilterFunction createOauthFilter(
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {

        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultClientRegistrationId("keycloak");
        oauth.setDefaultOAuth2AuthorizedClient(true);

        return oauth;
    }
}
