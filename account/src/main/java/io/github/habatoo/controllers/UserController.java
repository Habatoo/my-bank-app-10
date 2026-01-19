package io.github.habatoo.controllers;

import io.github.habatoo.dto.AccountFullResponseDto;
import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Mono<AccountFullResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getOrCreateUser(jwt);
    }

    @PatchMapping("/update")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ACCOUNT_ACCESS')")
    public Mono<AccountFullResponseDto> updateMe(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestBody UserUpdateDto dto) {
        return userService.updateProfile(jwt.getClaimAsString("preferred_username"), dto);
    }
}
