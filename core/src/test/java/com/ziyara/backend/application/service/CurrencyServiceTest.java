package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ExchangeRate;
import com.ziyara.backend.domain.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock ExchangeRateRepository exchangeRateRepository;
    @Mock ExchangeRateLookup exchangeRateLookup;

    CurrencyService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyService(exchangeRateRepository, exchangeRateLookup);
    }

    // ── convert ──────────────────────────────────────────────────────────[...]

    @Test
    void convert_sameCurrency_returnsSameAmount() {
        BigDecimal amount = BigDecimal.valueOf(100);

        BigDecimal result = service.convert(amount, "USD", "USD");

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void convert_rateFound_appliesRate() {
        ExchangeRate rate = new ExchangeRate();
        rate.setId(UUID.randomUUID());
        rate.setFromCurrency("USD");
        rate.setToCurrency("EUR");
        rate.setRate(BigDecimal.valueOf(0.9));

        when(exchangeRateLookup.getCachedRate("USD", "EUR"))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.convert(BigDecimal.valueOf(100), "USD", "EUR");

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(90.0));
    }

    @Test
    void convert_rateNotFound_throwsRuntimeException() {
        when(exchangeRateLookup.getCachedRate("USD", "JPY"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(BigDecimal.TEN, "USD", "JPY"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("JPY");
    }

    // ── convertOrKeep ────────────────────────────────────────────────────────[...]

    @Test
    void convertOrKeep_nullAmount_returnsZero() {
        BigDecimal result = service.convertOrKeep(null, "USD", "EUR");

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void convertOrKeep_nullFromCurrency_returnsAmountUnchanged() {
        BigDecimal amount = BigDecimal.valueOf(50);

        BigDecimal result = service.convertOrKeep(amount, null, "EUR");

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void convertOrKeep_nullToCurrency_returnsAmountUnchanged() {
        BigDecimal amount = BigDecimal.valueOf(50);

        BigDecimal result = service.convertOrKeep(amount, "USD", null);

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void convertOrKeep_sameCurrency_returnsAmountUnchanged() {
        BigDecimal amount = BigDecimal.valueOf(50);

        BigDecimal result = service.convertOrKeep(amount, "EUR", "EUR");

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void convertOrKeep_rateNotFound_returnsAmountUnchanged() {
        when(exchangeRateLookup.getCachedRate("USD", "GBP"))
                .thenReturn(Optional.empty());
        BigDecimal amount = BigDecimal.valueOf(75);

        BigDecimal result = service.convertOrKeep(amount, "USD", "GBP");

        assertThat(result).isEqualByComparingTo(amount);
    }
}
