package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class SystemSetting {

    private UUID id;
    private String settingKey;
    private String valueJson;
    private Instant updatedAt;
    private UUID updatedBy;

    public SystemSetting() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
