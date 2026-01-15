package io.github.habatoo.services;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Сервис по работе с пол
 */
public interface UserService {
    Mono<AccountFullResponseDto> getOrCreateUser(Jwt jwt);

    Mono<AccountFullResponseDto> updateProfile(String login, UserUpdateDto dto);
}
