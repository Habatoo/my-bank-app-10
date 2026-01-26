package io.github.habatoo.configurations;

import io.github.habatoo.handlers.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Конфигурация для бина перехвата исключений для микросервисов.
 */
@AutoConfiguration
public class ExceptionChassisAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
