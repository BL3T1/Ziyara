package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.ServiceImageCategory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: ServiceImage
 * Represents images associated with services or providers
 */
public class ServiceImage {
    
    private UUID id;
    private UUID serviceId;
    private String url;
    private String altText;
    private boolean isPrimary;
    private int displayOrder;
    private ServiceImageCategory category;
    private String contextKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ServiceImage() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isPrimary = false;
        this.displayOrder = 0;
        this.category = ServiceImageCategory.PROPERTY;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public ServiceImageCategory getCategory() { return category; }
    public void setCategory(ServiceImageCategory category) { this.category = category; }
    public String getContextKey() { return contextKey; }
    public void setContextKey(String contextKey) { this.contextKey = contextKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
