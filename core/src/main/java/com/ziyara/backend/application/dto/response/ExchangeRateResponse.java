package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rate details")
public class ExchangeRateResponse {
    
    @Schema(description = "Internal ID")
    private UUID id;
    
    @Schema(description = "Source currency")
    private String fromCurrency;
    
    @Schema(description = "Target currency")
    private String toCurrency;
    
    @Schema(description = "Conversion rate")
    private BigDecimal rate;
    
    @Schema(description = "Effective date")
    private LocalDate effectiveDate;

    public static ExchangeRateResponseBuilder builder() {
        return new ExchangeRateResponseBuilder();
    }

    public static class ExchangeRateResponseBuilder {
        private UUID id;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private LocalDate effectiveDate;

        public ExchangeRateResponseBuilder id(UUID id) { this.id = id; return this; }
        public ExchangeRateResponseBuilder fromCurrency(String v) { this.fromCurrency = v; return this; }
        public ExchangeRateResponseBuilder toCurrency(String v) { this.toCurrency = v; return this; }
        public ExchangeRateResponseBuilder rate(BigDecimal v) { this.rate = v; return this; }
        public ExchangeRateResponseBuilder effectiveDate(LocalDate v) { this.effectiveDate = v; return this; }
        public ExchangeRateResponse build() {
            ExchangeRateResponse r = new ExchangeRateResponse();
            r.setId(id); r.setFromCurrency(fromCurrency); r.setToCurrency(toCurrency);
            r.setRate(rate); r.setEffectiveDate(effectiveDate);
            return r;
        }
    }
}
