package io.github.habatoo.services;

import io.github.habatoo.dto.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты для проверки логики извлечения курсов из конфигурационной мапы.
 */
@DisplayName("Тестирование RateProviderService")
class RateProviderServiceTest {

    private RateProviderService rateProviderService;

    @BeforeEach
    void setUp() {
        rateProviderService = new RateProviderService();

        ReflectionTestUtils.setField(rateProviderService, "rubToUsd", "0.013");
        ReflectionTestUtils.setField(rateProviderService, "usdToRub", "76.38");

        rateProviderService.init();
    }

    @Test
    @DisplayName("Должен возвращать верный курс для существующей пары")
    void shouldReturnRateForValidPair() {
        BigDecimal rate = rateProviderService.getRate(Currency.RUB, Currency.USD);

        assertThat(rate).isEqualByComparingTo("0.013");
    }

    @Test
    @DisplayName("Должен возвращать 1, если валюты совпадают")
    void shouldReturnOneForSameCurrencies() {
        BigDecimal rate = rateProviderService.getRate(Currency.USD, Currency.USD);

        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, если пара не найдена")
    void shouldThrowExceptionWhenPairNotFound() {
        assertThatThrownBy(() -> rateProviderService.getRate(Currency.RUB, Currency.CNY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange rate not found");
    }
}
