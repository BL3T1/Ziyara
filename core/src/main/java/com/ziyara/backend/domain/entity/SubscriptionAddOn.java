package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.AddOnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain Entity: SubscriptionAddOn
 * An optional seat-expansion block attached to a CustomerSubscription.
 * The billing system activates/cancels these; the application only reads them
 * to determine effective seat capacity.
 */
public class SubscriptionAddOn {

    private UUID id;
    private UUID subscriptionId;
    private String addOnCode;
    private String displayName;
    private int extraSeats;
    private BigDecimal price;
    private AddOnStatus status;
    private Instant activatedAt;
    private Instant expiresAt;
    private Instant createdAt;

    public SubscriptionAddOn() {}

    public boolean isCurrentlyActive() {
        if (status == null || !status.isActive()) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    public UUID getId()                          { return id; }
    public void setId(UUID id)                   { this.id = id; }
    public UUID getSubscriptionId()              { return subscriptionId; }
    public void setSubscriptionId(UUID sid)      { this.subscriptionId = sid; }
    public String getAddOnCode()                 { return addOnCode; }
    public void setAddOnCode(String addOnCode)   { this.addOnCode = addOnCode; }
    public String getDisplayName()               { return displayName; }
    public void setDisplayName(String displayName){ this.displayName = displayName; }
    public int getExtraSeats()                   { return extraSeats; }
    public void setExtraSeats(int extraSeats)    { this.extraSeats = extraSeats; }
    public BigDecimal getPrice()                 { return price; }
    public void setPrice(BigDecimal price)       { this.price = price; }
    public AddOnStatus getStatus()               { return status; }
    public void setStatus(AddOnStatus status)    { this.status = status; }
    public Instant getActivatedAt()              { return activatedAt; }
    public void setActivatedAt(Instant t)        { this.activatedAt = t; }
    public Instant getExpiresAt()                { return expiresAt; }
    public void setExpiresAt(Instant t)          { this.expiresAt = t; }
    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant t)          { this.createdAt = t; }
}
