package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class FeatureFlag {

    private UUID id;
    private String flagKey;
    private boolean enabled;
    private String description;
    private Instant updatedAt;
    private UUID updatedBy;

    public FeatureFlag() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
