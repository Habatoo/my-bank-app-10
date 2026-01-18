package io.github.habatoo.cofigurations;

import io.github.habatoo.services.CashService;
import io.github.habatoo.services.FrontService;
import io.github.habatoo.services.TransferService;
import io.github.habatoo.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Тесты для проверки конфигурации безопасности приложения.
 * <p>
 * Проверяют корректность настройки прав доступа (permitAll/authenticated)
 * и работу цепочки фильтров безопасности WebFlux.
 * </p>
 */
@WebFluxTest
@Import(SecurityAutoConfiguration.class)
@DisplayName("Юнит-тесты конфигурации безопасности (SecurityAutoConfiguration)")
class SecurityAutoConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private FrontService frontService;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private UserService userService;

    /**
     * Проверяет, что доступ к защищенным ресурсам (например, /main)
     * перенаправляет неавторизованного пользователя на страницу входа.
     */
    @Test
    @DisplayName("Запрет доступа к защищенным ресурсам без авторизации (редирект на Login)")
    void shouldRedirectToLoginWhenUnauthenticatedTest() {
        webTestClient.get().uri("/main")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    /**
     * Проверяет доступ к защищенным ресурсам при наличии активной сессии.
     * <p>
     * Используется аннотация {@link WithMockUser} для имитации аутентифицированного пользователя.
     * </p>
     */
    @Test
    @WithMockUser
    @DisplayName("Доступ к главной странице для авторизованного пользователя")
    void shouldAllowAccessToMainWhenAuthenticatedTest() {
        webTestClient.get().uri("/main")
                .exchange()
                .expectStatus().isOk();
    }
}
