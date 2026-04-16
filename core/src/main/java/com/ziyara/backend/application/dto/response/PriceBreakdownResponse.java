package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Price breakdown (PRICING_METHODS.md).
 * Final Price = (Provider_Base - Disc_P - Disc_C) * (1 + Commission_Rate); tax inclusive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Price breakdown for preview or booking")
public class PriceBreakdownResponse {

    @Schema(description = "Base amount (before discounts, in service currency)")
    private BigDecimal baseAmount;

    @Schema(description = "Provider discount amount")
    private BigDecimal providerDiscountAmount;

    @Schema(description = "Company/promo discount amount")
    private BigDecimal companyDiscountAmount;

    @Schema(description = "Commission rate applied (e.g. 10 = 10%)")
    private BigDecimal commissionRate;

    @Schema(description = "Commission amount")
    private BigDecimal commissionAmount;

    @Schema(description = "Tax amount (inclusive)")
    private BigDecimal taxAmount;

    @Schema(description = "Total amount (customer pays)")
    private BigDecimal totalAmount;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Number of nights (hotels) or units")
    private Integer nights;

    @Schema(description = "Pricing model label (e.g. Per Night, Per Person)")
    private String pricingModel;

    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getProviderDiscountAmount() { return providerDiscountAmount; }
    public BigDecimal getCompanyDiscountAmount() { return companyDiscountAmount; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public Integer getNights() { return nights; }
    public String getPricingModel() { return pricingModel; }

    public static PriceBreakdownResponseBuilder builder() {
        return new PriceBreakdownResponseBuilder();
    }

    public static class PriceBreakdownResponseBuilder {
        private BigDecimal baseAmount;
        private BigDecimal providerDiscountAmount;
        private BigDecimal companyDiscountAmount;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String currency;
        private Integer nights;
        private String pricingModel;

        public PriceBreakdownResponseBuilder baseAmount(BigDecimal v) { this.baseAmount = v; return this; }
        public PriceBreakdownResponseBuilder providerDiscountAmount(BigDecimal v) { this.providerDiscountAmount = v; return this; }
        public PriceBreakdownResponseBuilder companyDiscountAmount(BigDecimal v) { this.companyDiscountAmount = v; return this; }
        public PriceBreakdownResponseBuilder commissionRate(BigDecimal v) { this.commissionRate = v; return this; }
        public PriceBreakdownResponseBuilder commissionAmount(BigDecimal v) { this.commissionAmount = v; return this; }
        public PriceBreakdownResponseBuilder taxAmount(BigDecimal v) { this.taxAmount = v; return this; }
        public PriceBreakdownResponseBuilder totalAmount(BigDecimal v) { this.totalAmount = v; return this; }
        public PriceBreakdownResponseBuilder currency(String v) { this.currency = v; return this; }
        public PriceBreakdownResponseBuilder nights(Integer v) { this.nights = v; return this; }
        public PriceBreakdownResponseBuilder pricingModel(String v) { this.pricingModel = v; return this; }
        public PriceBreakdownResponse build() {
            PriceBreakdownResponse r = new PriceBreakdownResponse();
            r.setBaseAmount(baseAmount); r.setProviderDiscountAmount(providerDiscountAmount);
            r.setCompanyDiscountAmount(companyDiscountAmount); r.setCommissionRate(commissionRate);
            r.setCommissionAmount(commissionAmount); r.setTaxAmount(taxAmount); r.setTotalAmount(totalAmount);
            r.setCurrency(currency); r.setNights(nights); r.setPricingModel(pricingModel);
            return r;
        }
    }
}
