package com.ziyara.backend.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class ProviderDiscountBalance {

    private UUID providerId;
    private String currency;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;

    public BigDecimal getAvailableAmount() {
        if (allocatedAmount == null) return BigDecimal.ZERO;
        if (spentAmount == null) return allocatedAmount;
        return allocatedAmount.subtract(spentAmount);
    }

    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
}
