package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class PortalSupportRequest {

    private UUID id;
    private UUID providerId;
    private UUID userId;
    private String subject;
    private String body;
    private Instant createdAt;

    public PortalSupportRequest() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
