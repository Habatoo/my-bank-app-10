package io.github.habatoo.services.impl;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.PasswordUpdateDto;
import io.github.habatoo.dto.UserProfileResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
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

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    private WebClient.RequestHeadersSpec headersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserServiceImpl userService;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("preferred_username")).thenReturn(LOGIN);
        ReflectionTestUtils.setField(userService, "keycloakIssuerUri", "http://localhost:8082/realms/bank");
    }

    /**
     * Тест получения данных уже существующего в базе пользователя.
     */
    @Test
    @DisplayName("Получение пользователя: возврат существующего профиля (без счетов)")
    void getOrCreateUserExistingTest() {
        User existingUser = User.builder()
                .id(USER_ID)
                .login(LOGIN)
                .name("Existing User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.just(existingUser));
        when(accountRepository.findAllByUserId(USER_ID)).thenReturn(Flux.empty());

        Mono<UserProfileResponseDto> result = userService.getOrCreateUser(jwt);

        StepVerifier.create(result)
                .expectNextMatches(dto ->
                        dto.getLogin().equals(LOGIN) &&
                                dto.getAccounts().isEmpty() &&
                                dto.getName().equals("Existing User"))
                .verifyComplete();

        verify(userRepository, never()).save(any());
        verify(accountRepository).findAllByUserId(USER_ID);
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

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(outboxClientService.saveEvent(any())).thenReturn(Mono.empty());

        Mono<UserProfileResponseDto> result = userService.getOrCreateUser(jwt);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getLogin().equals(LOGIN))
                .verifyComplete();

        verify(userRepository).save(any(User.class));
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

    /**
     * Тест обновления пароля: успешный сценарий.
     */
    @Test
    @DisplayName("Обновление пароля: успех")
    void updatePasswordSuccessTest() {
        PasswordUpdateDto dto = new PasswordUpdateDto("newPass", "newPass");
        String userId = "kc-uuid-123";

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), eq(LOGIN))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(Map.of("id", userId, "username", LOGIN)));

        WebClient.RequestBodyUriSpec putUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(anyString(), eq(userId))).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);

        when(putResponseSpec.onStatus(any(), any())).thenReturn(putResponseSpec);
        when(putResponseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        Mono<Boolean> result = userService.updatePassword(LOGIN, dto);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * Тест обновления профиля: случай, когда у пользователя нет счета в RUB.
     * Важно проверить, что цепочка не прервется ошибкой, а просто вернет пустой Mono (или ошибку).
     */
    @Test
    @DisplayName("Обновление профиля: счет в RUB не найден")
    void updateProfileAccountNotFoundTest() {
        User user = User.builder().id(USER_ID).login(LOGIN).name("Ivan").build();
        UserUpdateDto updateDto = new UserUpdateDto("New Name", LocalDate.of(1990, 1, 1));

        when(userRepository.findByLogin(LOGIN)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(accountRepository.findByUserIdAndCurrency(USER_ID, Currency.RUB)).thenReturn(Mono.empty());

        Mono<AccountFullResponseDto> result = userService.updateProfile(LOGIN, updateDto);

        StepVerifier.create(result)
                .verifyComplete();

        verify(outboxClientService, never()).saveEvent(any());
    }

    /**
     * Тест обновления пароля: имитация ошибки Keycloak.
     */
    @Test
    @DisplayName("Обновление пароля: возврат false при ошибке API")
    void updatePasswordKeycloakErrorTest() {
        String login = "test_user";
        PasswordUpdateDto dto = new PasswordUpdateDto("old", "new");

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), eq(login))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.error(new RuntimeException("Access Denied")));

        Mono<Boolean> result = userService.updatePassword(login, dto);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }
}
