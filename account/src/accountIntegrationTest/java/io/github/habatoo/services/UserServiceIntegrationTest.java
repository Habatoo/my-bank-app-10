package io.github.habatoo.services;

import io.github.habatoo.BaseAccountTest;
import io.github.habatoo.dto.NotificationEvent;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Интеграционный тест для {@link UserService}.
 * Проверяет создание пользователей из JWT и обновление профиля с сохранением событий в Outbox.
 */
@DisplayName("Интеграционное тестирование UserService")
class UserServiceIntegrationTest extends BaseAccountTest {

    @BeforeEach
    void setUp() {
        when(outboxClientService.saveEvent(ArgumentMatchers.any(NotificationEvent.class)))
                .thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("getOrCreateUser: Регистрация нового пользователя при отсутствии в БД")
    void getOrCreateUserShouldRegisterNewUserTest() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("new_user");
        when(jwt.getClaimAsString("given_name")).thenReturn("John Doe");
        when(jwt.getClaim("birthdate")).thenReturn("1995-05-15");
        when(jwt.getClaimAsString("birthdate")).thenReturn("1995-05-15");
        when(jwt.getClaim("initialSum")).thenReturn("500.00");
        when(jwt.getClaimAsString("initialSum")).thenReturn("500.00");
        var testAction = Mono.defer(() -> userService.getOrCreateUser(jwt));

        StepVerifier.create(testAction)
                .assertNext(dto -> {
                    assertThat(dto.getLogin()).isEqualTo("new_user");
                    assertThat(dto.getName()).isEqualTo("John Doe");
                    assertThat(dto.getBalance()).isEqualByComparingTo("500.00");
                })
                .verifyComplete();

        StepVerifier.create(userRepository.findByLogin("new_user"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProfile: Успешное обновление данных и отправка уведомления")
    void updateProfileShouldUpdateDataAndNotifyTest() {
        String login = "update_me";
        User user = createUser(login);
        UserUpdateDto updateDto = createUserUpdateDto("New Name");

        var setup = clearDatabase()
                .then(userRepository.save(user))
                .flatMap(u -> accountRepository.save(
                        Account.builder().userId(u.getId()).balance(BigDecimal.ZERO).currency(Currency.RUB).build()));

        StepVerifier.create(setup.then(userService.updateProfile(login, updateDto)))
                .assertNext(dto -> {
                    assertThat(dto.getName()).isEqualTo("New Name");
                    assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
                })
                .verifyComplete();

        verify(outboxClientService).saveEvent(ArgumentMatchers.argThat(event ->
                event.getEventType().equals(io.github.habatoo.dto.enums.EventType.UPDATE_PROFILE) &&
                        event.getUsername().equals(login)
        ));
    }

    @Test
    @DisplayName("getOrCreateUser: Возврат существующего пользователя без создания нового")
    void getOrCreateUserShouldReturnExistingUserTest() {
        String login = "existing";
        User user = createUser(login);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(login);

        var setup = clearDatabase()
                .then(userRepository.save(user))
                .flatMap(u -> accountRepository.save(
                        Account.builder().userId(u.getId()).balance(BigDecimal.TEN).currency(Currency.RUB).build()));

        StepVerifier.create(setup.then(userService.getOrCreateUser(jwt)))
                .assertNext(dto -> {
                    assertThat(dto.getLogin()).isEqualTo(login);
                    assertThat(dto.getBalance()).isEqualByComparingTo("10.00");
                })
                .verifyComplete();

        StepVerifier.create(userRepository.findAll().collectList())
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();
    }
}
