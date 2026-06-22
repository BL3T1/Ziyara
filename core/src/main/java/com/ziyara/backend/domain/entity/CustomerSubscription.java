package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Entity: CustomerSubscription
 * Represents a ServiceProvider's active plan agreement.
 * One non-cancelled subscription per provider at a time (enforced by DB unique index).
 */
public class CustomerSubscription {

    private UUID id;
    private UUID providerId;
    private UUID planId;
    private SubscriptionStatus status;
    /** Hard seat ceiling from the plan at the time of subscription. */
    private int seatLimit;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialEndsAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** Populated by SubscriptionService (not persisted on this entity). */
    private List<SubscriptionAddOn> addOns;

    public CustomerSubscription() {}

    /**
     * Total effective seat ceiling: base seatLimit + sum of active add-on extra seats.
     */
    public int effectiveSeatLimit() {
        int extra = 0;
        if (addOns != null) {
            for (SubscriptionAddOn addOn : addOns) {
                if (addOn.getStatus() != null && addOn.getStatus().isActive()) {
                    extra += addOn.getExtraSeats();
                }
            }
        }
        return seatLimit + extra;
    }

    public boolean isUsable() {
        return status != null && status.isUsable();
    }

    public UUID getId()                                   { return id; }
    public void setId(UUID id)                            { this.id = id; }
    public UUID getProviderId()                           { return providerId; }
    public void setProviderId(UUID providerId)            { this.providerId = providerId; }
    public UUID getPlanId()                               { return planId; }
    public void setPlanId(UUID planId)                    { this.planId = planId; }
    public SubscriptionStatus getStatus()                 { return status; }
    public void setStatus(SubscriptionStatus status)      { this.status = status; }
    public int getSeatLimit()                             { return seatLimit; }
    public void setSeatLimit(int seatLimit)               { this.seatLimit = seatLimit; }
    public Instant getCurrentPeriodStart()                { return currentPeriodStart; }
    public void setCurrentPeriodStart(Instant start)      { this.currentPeriodStart = start; }
    public Instant getCurrentPeriodEnd()                  { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant end)          { this.currentPeriodEnd = end; }
    public Instant getTrialEndsAt()                       { return trialEndsAt; }
    public void setTrialEndsAt(Instant trialEndsAt)       { this.trialEndsAt = trialEndsAt; }
    public Instant getCancelledAt()                       { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt)       { this.cancelledAt = cancelledAt; }
    public Instant getCreatedAt()                         { return createdAt; }
    public void setCreatedAt(Instant createdAt)           { this.createdAt = createdAt; }
    public Instant getUpdatedAt()                         { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)           { this.updatedAt = updatedAt; }
    public List<SubscriptionAddOn> getAddOns()            { return addOns; }
    public void setAddOns(List<SubscriptionAddOn> addOns) { this.addOns = addOns; }
}
