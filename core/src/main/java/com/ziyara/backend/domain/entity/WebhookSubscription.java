package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class WebhookSubscription {

    private UUID id;
    private UUID providerId;
    private String name;
    private String url;
    private List<String> events;
    private String secret;
    private boolean active;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
