package com.ziyara.backend.domain.enums;

public enum AddOnStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
