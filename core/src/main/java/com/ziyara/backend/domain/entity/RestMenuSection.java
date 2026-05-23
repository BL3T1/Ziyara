package com.ziyara.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class RestMenuSection {

    private UUID id;
    private UUID serviceId;
    private String title;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RestMenuSection() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
