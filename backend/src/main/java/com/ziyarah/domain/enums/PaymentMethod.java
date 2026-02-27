package com.ziyarah.domain.enums;

/**
 * Payment Method Enumeration
 * Supported payment options in the system
 */
public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    PAYPAL("PayPal"),
    BANK_TRANSFER("Bank Transfer"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    CASH_ON_ARRIVAL("Cash on Arrival"),
    WALLET("Digital Wallet");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
