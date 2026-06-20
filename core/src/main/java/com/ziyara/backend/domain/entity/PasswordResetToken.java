package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;

    public PasswordResetToken() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
