package io.github.habatoo.controllers;

import io.github.habatoo.AccountApplication;
import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.AccountShortDto;
import io.github.habatoo.dto.OperationResultDto;
import io.github.habatoo.dto.UserUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;

@SpringBootTest(
        classes = AccountApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class AccountControllerContractTest {

    @LocalServerPort
    int port;

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Получение списка всех аккаунтов (кроме текущего пользователя)
     */
    @Test
    void getUsersContract() {
        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBodyList(AccountShortDto.class);
    }

    /**
     * Изменение баланса пользователя администратором
     */
    @Test
    void updateBalanceContract() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("login", "user1")
                        .queryParam("amount", "100.50")
                        .build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(OperationResultDto.class);
    }

    /**
     * Получение информации о текущем пользователе
     */
    @Test
    void getCurrentUserContract() {
        webTestClient.get()
                .uri("/user")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(AccountFullResponseDto.class);
    }

    /**
     * Обновление данных профиля текущего пользователя
     */
    @Test
    void updateProfileContract() {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setName("Иван");
        updateDto.setBirthDate(LocalDate.of(1990, 5, 15));

        webTestClient.patch()
                .uri("/update")
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(AccountFullResponseDto.class);
    }

    @Configuration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(auth -> auth.anyExchange().permitAll())
                    .build();
        }
    }
}
