package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.ProviderStaffRole;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProviderStaff {

    private UUID id;
    private UUID providerId;
    private UUID userId;
    private String title;
    private ProviderStaffRole providerRole;
    private LocalDateTime createdAt;

    public ProviderStaff() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ProviderStaffRole getProviderRole() { return providerRole; }
    public void setProviderRole(ProviderStaffRole providerRole) { this.providerRole = providerRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
