package com.ziyara.backend.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain Entity: Plan
 * Describes a subscription tier (FREE, STARTER, PROFESSIONAL, ENTERPRISE).
 * max_users == -1 means unlimited seats.
 */
public class Plan {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private int maxUsers;
    private BigDecimal monthlyPrice;
    private String currency;
    private boolean allowsOverage;
    private BigDecimal overagePricePerUser;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Plan() {}

    /** Returns true if the plan has no hard user cap. */
    public boolean isUnlimited() {
        return maxUsers == -1;
    }

    /** Effective seat ceiling: unlimited plans return Integer.MAX_VALUE. */
    public int effectiveMaxUsers() {
        return isUnlimited() ? Integer.MAX_VALUE : maxUsers;
    }

    public UUID getId()                        { return id; }
    public void setId(UUID id)                 { this.id = id; }
    public String getCode()                    { return code; }
    public void setCode(String code)           { this.code = code; }
    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }
    public String getDescription()             { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMaxUsers()                   { return maxUsers; }
    public void setMaxUsers(int maxUsers)      { this.maxUsers = maxUsers; }
    public BigDecimal getMonthlyPrice()        { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public String getCurrency()                { return currency; }
    public void setCurrency(String currency)   { this.currency = currency; }
    public boolean isAllowsOverage()           { return allowsOverage; }
    public void setAllowsOverage(boolean allowsOverage) { this.allowsOverage = allowsOverage; }
    public BigDecimal getOveragePricePerUser() { return overagePricePerUser; }
    public void setOveragePricePerUser(BigDecimal overagePricePerUser) { this.overagePricePerUser = overagePricePerUser; }
    public boolean isActive()                  { return active; }
    public void setActive(boolean active)      { this.active = active; }
    public Instant getCreatedAt()              { return createdAt; }
    public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
    public Instant getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt){ this.updatedAt = updatedAt; }
}
