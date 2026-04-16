package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.DiscountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private UUID createdBy;
    /** {@link com.ziyara.backend.domain.enums.DiscountSponsor} code: COMPANY, PROVIDER, BOTH */
    private String sponsor;
    /** When set, code applies only to this provider's listings. */
    private UUID providerId;
    /** When non-empty, code applies only to these service (listing) IDs. */
    private List<UUID> applicableServiceIds;
    /** Restaurant: restrict to these menu section IDs (OR with applicable menu items when both set). */
    private List<UUID> applicableMenuSectionIds;
    private List<UUID> applicableMenuItemIds;
    /** Hotel/resort: restrict to these room-type IDs when the client sends {@code roomTypeId} on checkout. */
    private List<UUID> applicableRoomTypeIds;

    // Domain behavior methods
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        if (status != DiscountStatus.ACTIVE) {
            return false;
        }
        if (endDate == null || !now.isBefore(endDate)) {
            return false;
        }
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        return usageLimit == 0 || usageCount < usageLimit;
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (!isValid() || amount.compareTo(minBookingAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(type)) {
            discount = amount.multiply(value).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
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
        this.sponsor = "COMPANY";
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getMinBookingAmount() {
        return minBookingAmount;
    }

    public void setMinBookingAmount(BigDecimal minBookingAmount) {
        this.minBookingAmount = minBookingAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public DiscountStatus getStatus() {
        return status;
    }

    public void setStatus(DiscountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getSponsor() {
        return sponsor;
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public List<UUID> getApplicableServiceIds() {
        return applicableServiceIds;
    }

    public void setApplicableServiceIds(List<UUID> applicableServiceIds) {
        this.applicableServiceIds = applicableServiceIds;
    }

    public List<UUID> getApplicableMenuSectionIds() {
        return applicableMenuSectionIds;
    }

    public void setApplicableMenuSectionIds(List<UUID> applicableMenuSectionIds) {
        this.applicableMenuSectionIds = applicableMenuSectionIds;
    }

    public List<UUID> getApplicableMenuItemIds() {
        return applicableMenuItemIds;
    }

    public void setApplicableMenuItemIds(List<UUID> applicableMenuItemIds) {
        this.applicableMenuItemIds = applicableMenuItemIds;
    }

    public List<UUID> getApplicableRoomTypeIds() {
        return applicableRoomTypeIds;
    }

    public void setApplicableRoomTypeIds(List<UUID> applicableRoomTypeIds) {
        this.applicableRoomTypeIds = applicableRoomTypeIds;
    }
}
