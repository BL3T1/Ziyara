package com.ziyarah.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: Department
 * Represents organizational divisions within the company
 */
public class Department {
    
    private UUID id;
    private String name;
    private String description;
    private UUID managerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Department() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
