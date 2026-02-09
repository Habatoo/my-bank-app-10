package io.github.habatoo.configurations;

import io.github.habatoo.services.RateClientService;
import io.github.habatoo.services.RateProviderService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Автоконфигурация сервисного слоя шасси микросервисов,
 * отвечает за регистрацию инфраструктурных сервисов курсов валют.
 */
@AutoConfiguration
public class RateChassisAutoConfiguration {

    /**
     * Создает бин сервиса провайдера данных,
     * загружающий курсы валют из конфигурационных файлов.
     *
     * @return настроенный экземпляр {@link RateProviderService}.
     */
    @Bean
    @ConfigurationProperties(prefix = "rate")
    public RateProviderService rateProviderService() {
        return new RateProviderService();
    }


    /**
     * Создает бин сервиса для получения курсов конвертации между валютами.
     *
     * @param rateProviderService клиент провайдера данных курсов валют.
     * @return настроенный экземпляр {@link RateClientService}.
     */
    @Bean
    public RateClientService rateClientService(RateProviderService rateProviderService) {
        return new RateClientService(rateProviderService);
    }
}
