package io.github.habatoo.services;

import io.github.habatoo.dto.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки логики принятия решения о выборе курса.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование RateClientService")
class RateClientServiceTest {

    @Mock
    private RateProviderService rateProviderService;

    @InjectMocks
    private RateClientService rateClientService;

    @Test
    @DisplayName("Должен возвращать единицу, если валюты одинаковые")
    void shouldReturnOneForSameCurrencies() {
        BigDecimal rate = rateClientService.takeRate(Currency.RUB, Currency.RUB);
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Должен обращаться к провайдеру, если валюты разные")
    void shouldCallProviderForDifferentCurrencies() {
        BigDecimal expectedRate = new BigDecimal("0.091");
        when(rateProviderService.getRate(Currency.RUB, Currency.CNY)).thenReturn(expectedRate);

        BigDecimal actualRate = rateClientService.takeRate(Currency.RUB, Currency.CNY);

        assertThat(actualRate).isEqualByComparingTo(expectedRate);
    }
}
