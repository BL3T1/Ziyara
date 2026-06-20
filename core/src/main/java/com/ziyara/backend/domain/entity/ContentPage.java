package com.ziyara.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContentPage {

    private UUID id;
    private String slug;
    private Map<String, Object> contentEn = new HashMap<>();
    private Map<String, Object> contentAr = new HashMap<>();
    private Boolean published = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContentPage() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Map<String, Object> getContentEn() { return contentEn; }
    public void setContentEn(Map<String, Object> contentEn) { this.contentEn = contentEn; }

    public Map<String, Object> getContentAr() { return contentAr; }
    public void setContentAr(Map<String, Object> contentAr) { this.contentAr = contentAr; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
