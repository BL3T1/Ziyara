package com.ziyarah.domain.entity;

import com.ziyarah.domain.enums.DiscountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: DiscountCode
 * Represents promotional codes and coupons
 */
public class DiscountCode {
    
    private UUID id;
    private String code;
    private String description;
    private String type; // e.g., PERCENTAGE, FIXED_AMOUNT
    private BigDecimal value;
    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int usageLimit;
    private int usageCount;
    private DiscountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior methods
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == DiscountStatus.ACTIVE && 
               now.isAfter(startDate) && 
               now.isBefore(endDate) && 
               (usageLimit == 0 || usageCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (!isValid() || amount.compareTo(minBookingAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(type)) {
            discount = amount.multiply(value).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = value;
        }
        
        return discount.min(amount); // Discount cannot exceed amount
    }

    // Constructors
    public DiscountCode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = DiscountStatus.ACTIVE;
        this.usageCount = 0;
        this.minBookingAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMinBookingAmount() { return minBookingAmount; }
    public void setMinBookingAmount(BigDecimal minBookingAmount) { this.minBookingAmount = minBookingAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public DiscountStatus getStatus() { return status; }
    public void setStatus(DiscountStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
