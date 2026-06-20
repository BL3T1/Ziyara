package com.ziyara.backend.domain.enums;

/**
 * Lifecycle states for a {@code CashCollection}.
 * OPEN — provider has recorded cash; awaiting admin reconciliation.
 * RECONCILED — admin has matched cash against platform commission settlement.
 * DISPUTED — admin has flagged a discrepancy; requires manual finance review.
 */
public enum CashCollectionStatus {
    OPEN("Open"),
    RECONCILED("Reconciled"),
    DISPUTED("Disputed");

    private final String displayName;

    CashCollectionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
