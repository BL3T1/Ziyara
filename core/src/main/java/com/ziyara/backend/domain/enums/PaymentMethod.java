package com.ziyara.backend.domain.enums;

/**
 * Payment Method Enumeration
 * Aligned with PAYMENT_METHODS.md: Visa/Card, Bank Transfer, Z-Pay Wallet, Cash on Service
 */
public enum PaymentMethod {
    CREDIT_CARD("Visa / Credit Card"),
    DEBIT_CARD("Debit Card"),
    BANK_TRANSFER("Bank Transfer"),
    WALLET("Z-Pay Wallet"),
    CASH_ON_SERVICE("Cash on Service"),
    CASH_ON_ARRIVAL("Cash on Arrival"),
    PAYPAL("PayPal"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    CRYPTO_WALLET("Crypto Wallet"),
    CASH("Cash"),
    CHEQUE("Cheque"),
    OTHER("Other");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
