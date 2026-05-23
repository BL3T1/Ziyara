package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class OtpVerification {

    private UUID id;
    private String emailOrPhone;
    private String otp;
    private Instant expiresAt;
    private Instant createdAt;

    public OtpVerification() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmailOrPhone() { return emailOrPhone; }
    public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
