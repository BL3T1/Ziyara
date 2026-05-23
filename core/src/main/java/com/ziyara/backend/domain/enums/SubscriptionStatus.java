package com.ziyara.backend.domain.enums;

public enum SubscriptionStatus {
    TRIAL,
    ACTIVE,
    PAST_DUE,
    CANCELLED,
    EXPIRED;

    public boolean isUsable() {
        return this == TRIAL || this == ACTIVE;
    }
}
