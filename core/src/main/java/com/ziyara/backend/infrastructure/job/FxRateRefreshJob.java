package com.ziyara.backend.infrastructure.job;

import com.ziyara.backend.domain.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * Nightly FX rate auto-refresh from exchangerate.host.
 *
 * <p>Enabled by setting {@code ZIYARA_FX_REFRESH_ENABLED=true} and providing an API key
 * via {@code ZIYARA_FX_REFRESH_API_KEY}. When disabled the job bean is not registered, so
 * there is no runtime cost in development.
 *
 * <p>Uses PostgreSQL {@code ON CONFLICT ... DO UPDATE} (via
 * {@link ExchangeRateRepository#upsert}) — safe to run multiple times on the same day.
 */
@Component
@ConditionalOnProperty(name = "ziyara.fx.refresh.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FxRateRefreshJob {

    @Value("${ziyara.fx.refresh.api-key:}")
    private String apiKey;

    @Value("${ziyara.fx.refresh.base-currency:USD}")
    private String baseCurrency;

    @Value("${ziyara.fx.refresh.target-currencies:EUR,SAR,GBP,AED}")
    private String targetCurrencies;

    private final ExchangeRateRepository exchangeRateRepository;
    private final CacheManager cacheManager;
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "${ziyara.fx.refresh.cron:0 0 6 * * *}", zone = "UTC")
    public void refresh() {
        log.info("[FxRefresh] Starting rate refresh — base={} targets={}", baseCurrency, targetCurrencies);
        try {
            // exchangerate.host /live endpoint: GET ?access_key=X&source=USD&currencies=EUR,SAR,...
            String url = "https://api.exchangerate.host/live"
                    + "?access_key=" + apiKey
                    + "&source=" + baseCurrency
                    + "&currencies=" + targetCurrencies;

            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);

            if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                log.warn("[FxRefresh] API returned error or null — response: {}", body);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Number> quotes = (Map<String, Number>) body.get("quotes");
            if (quotes == null || quotes.isEmpty()) {
                log.warn("[FxRefresh] API returned empty quotes");
                return;
            }

            LocalDate today = LocalDate.now();
            int updated = 0;
            for (Map.Entry<String, Number> entry : quotes.entrySet()) {
                // Quote key format: "USDEUR", "USDSAR" — strip the base prefix
                String pair = entry.getKey();
                String toCurrency = pair.length() > 3 ? pair.substring(3) : pair;
                BigDecimal rate = new BigDecimal(entry.getValue().toString())
                        .setScale(6, RoundingMode.HALF_UP);
                exchangeRateRepository.upsert(baseCurrency, toCurrency, rate, today);
                log.debug("[FxRefresh] {} → {} = {}", baseCurrency, toCurrency, rate);
                updated++;
            }
            log.info("[FxRefresh] Done — updated {} currency pairs for {}", updated, today);
            var cache = cacheManager.getCache("exchangeRates");
            if (cache != null) cache.clear();

        } catch (Exception ex) {
            log.error("[FxRefresh] Failed to refresh FX rates: {}", ex.getMessage(), ex);
        }
    }
}
