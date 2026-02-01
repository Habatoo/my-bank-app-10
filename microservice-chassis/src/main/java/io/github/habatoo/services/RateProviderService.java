package io.github.habatoo.services;

import io.github.habatoo.dto.enums.Currency;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Провайдер данных, загружающий курсы валют из конфигурационных файлов (prefix = "rate").
 * Хранит котировки в виде карты, где ключ формируется по шаблону "from_to_to".
 */
@Slf4j
@Getter
public class RateProviderService {

    @Value("${rate.rub_to_usd:0.013}")
    private String rubToUsd;

    @Value("${rate.rub_to_cny:0.091}")
    private String rubToCny;

    @Value("${rate.cny_to_usd:0.14}")
    private String cnyToUsd;

    @Value("${rate.cny_to_rub:10.99}")
    private String cnyToRub;

    @Value("${rate.usd_to_rub:76.38}")
    private String usdToRub;

    @Value("${rate.usd_to_cny:6.95}")
    private String usdToCny;

    private final Map<String, BigDecimal> internalRates = new HashMap<>();

    /**
     * Инициализация мапы из полей @Value при старте бина.
     */
    @PostConstruct
    public void init() {
        addRateSafely("rub_to_usd", rubToUsd);
        addRateSafely("rub_to_cny", rubToCny);
        addRateSafely("cny_to_usd", cnyToUsd);
        addRateSafely("cny_to_rub", cnyToRub);
        addRateSafely("usd_to_rub", usdToRub);
        addRateSafely("usd_to_cny", usdToCny);

        log.info("RateProviderService инициализирован. Загружено курсов: {}", internalRates.size());
    }

    /**
     * Получение курса.
     */
    public BigDecimal getRate(Currency from, Currency to) {
        if (from == to) return BigDecimal.ONE;

        String key = (from.name() + "_to_" + to.name()).toLowerCase();
        BigDecimal rate = internalRates.get(key);

        if (rate == null) {
            log.error("Курс не найден для пары: {}. Проверьте значения в @Value.", key);
            throw new IllegalArgumentException("Exchange rate not found for " + key);
        }
        return rate;
    }

    private void addRateSafely(String key, String value) {
        if (value != null && !value.isBlank()) {
            internalRates.put(key, new BigDecimal(value));
        } else {
            log.warn("Курс для {} не задан в конфигурации", key);
        }
    }
}
