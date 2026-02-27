package com.ziyarah.domain.enums;

/**
 * Notification Channel Enumeration
 * Specifies the delivery method for notifications
 */
public enum NotificationChannel {
    EMAIL("Email"),
    SMS("SMS"),
    PUSH("Push Notification"),
    IN_APP("In-App Message"),
    WHATSAPP("WhatsApp");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
