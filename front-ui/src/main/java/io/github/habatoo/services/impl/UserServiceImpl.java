package io.github.habatoo.services.impl;

import io.github.habatoo.dto.UserUpdateDto;
import io.github.habatoo.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static io.github.habatoo.constants.ApiConstants.BASE_GATEWAY_URL;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final WebClient webClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RedirectView> updateProfile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String name = formData.getFirst("name");
                    String birthDateStr = formData.getFirst("birthdate");

                    if (name == null || birthDateStr == null) {
                        return Mono.just(new RedirectView("/main?error=MissingFields"));
                    }

                    UserUpdateDto updateDto = new UserUpdateDto(name, LocalDate.parse(birthDateStr));

                    return webClient.patch()
                            .uri(BASE_GATEWAY_URL + "/account/update")
                            .bodyValue(updateDto)
                            .retrieve()
                            .toBodilessEntity()
                            .map(response -> getRedirectView())
                            .onErrorResume(e -> {
                                log.error("Update error: ", e);
                                return Mono.just(new RedirectView("/main?error=UpdateFailed"));
                            });
                });
    }

    private RedirectView getRedirectView() {
        String info = UriComponentsBuilder.fromPath("")
                .queryParam("info", "Профиль обновлен")
                .build().encode().toUriString();
        return new RedirectView("/main" + info);
    }
}
