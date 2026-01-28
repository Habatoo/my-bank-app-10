package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.dto.enums.Currency;
import io.github.habatoo.models.Account;
import io.github.habatoo.models.User;
import io.github.habatoo.repositories.AccountRepository;
import io.github.habatoo.repositories.UserRepository;
import io.github.habatoo.services.OutboxClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестирование реализации сервиса пользователей {@link UserServiceImpl}.
 * Проверяет логику автоматической регистрации из JWT и обновление профиля.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты сервиса UserServiceImpl")
class UserServiceImplTest {

    private final String LOGIN = "test_user";
    private final UUID USER_ID = UUID.randomUUID();
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OutboxClientService outboxClientService;
    @InjectMocks
    private UserServiceImpl userService;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("preferred_username")).thenReturn(LOGIN);
    }

    /**
     * Тест получения данных уже существующего в базе пользователя.
     */
    @Test
    @DisplayName("Получение пользователя: возврат существующего профиля")
    void getOrCreateUserExistingTest() {
        User existingUser = User.builder()
                .id(USER_ID)
                .login(LOGIN)
                .name("Existing User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        Account existingAccount = Account.builder()
                .userId(USER_ID)
                .balance(BigDecimal.TEN)
                .build();

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.just(existingUser));
        when(accountRepository.findByUserIdAndCurrency(USER_ID, Currency.RUB)).thenReturn(Mono.just(existingAccount));

        Mono<AccountFullResponseDto> result = userService.getOrCreateUser(jwt);

        StepVerifier.create(result)
                .expectNextMatches(dto ->
                        dto.getLogin().equals(LOGIN) &&
                                dto.getBalance().compareTo(BigDecimal.TEN) == 0 &&
                                dto.getName().equals("Existing User"))
                .verifyComplete();

        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    /**
     * Тест сценария "ленивой регистрации", когда пользователя нет в базе.
     */
    @Test
    @DisplayName("Получение пользователя: регистрация нового пользователя из JWT")
    void getOrCreateUserRegistrationTest() {
        when(jwt.getClaimAsString("given_name")).thenReturn("New User");
        when(jwt.getClaim("birthdate")).thenReturn("1990-01-01");
        when(jwt.getClaimAsString("birthdate")).thenReturn("1990-01-01");
        when(jwt.getClaim("initialSum")).thenReturn("100.00");
        when(jwt.getClaimAsString("initialSum")).thenReturn("100.00");

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<AccountFullResponseDto> result = userService.getOrCreateUser(jwt);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getLogin().equals(LOGIN)
                        && dto.getBalance().compareTo(new BigDecimal("100.00")) == 0)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
        verify(accountRepository).save(any(Account.class));
        verify(outboxClientService).saveEvent(
                argThat(e -> e.getEventType().name().equals("REGISTRATION")));
    }

    /**
     * Тест успешного обновления полей профиля.
     */
    @Test
    @DisplayName("Обновление профиля: успех")
    void updateProfileSuccessTest() {
        User user = User.builder().id(USER_ID).login(LOGIN).name("Old Name").build();
        Account account = Account.builder().userId(USER_ID).balance(BigDecimal.ZERO).build();
        UserUpdateDto updateDto = new UserUpdateDto("New Name", LocalDate.of(1990, 1, 1));

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(accountRepository.findByUserIdAndCurrency(USER_ID, Currency.RUB)).thenReturn(Mono.just(account));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<AccountFullResponseDto> result = userService.updateProfile(LOGIN, updateDto);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getName().equals("New Name"))
                .verifyComplete();

        verify(userRepository).save(argThat(u -> u.getName().equals("New Name")));
        verify(outboxClientService).saveEvent(
                argThat(e -> e.getEventType().name().equals("UPDATE_PROFILE")));
    }
}
