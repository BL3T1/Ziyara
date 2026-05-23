package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SecurityAlert {

    private UUID id;
    private UUID ruleId;
    private UUID userId;
    private Map<String, Object> triggeredBy;
    private Integer occurrenceCount;
    private String severity;
    private String status;
    private Instant createdAt;

    public SecurityAlert() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Map<String, Object> getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(Map<String, Object> triggeredBy) { this.triggeredBy = triggeredBy; }

    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
