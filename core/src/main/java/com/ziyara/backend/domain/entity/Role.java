package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain Entity: Role (RBAC)
 */
public class Role {
    private UUID id;
    private String name;
    private String nameAr;
    private String code;
    private String description;
    private String descriptionAr;
    private RoleLevel level;
    private UUID groupId;
    private boolean systemRole;
    private RoleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> navigationItemIds;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDescriptionAr() { return descriptionAr; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }
    public RoleLevel getLevel() { return level; }
    public void setLevel(RoleLevel level) { this.level = level; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }
    public RoleStatus getStatus() { return status; }
    public void setStatus(RoleStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getNavigationItemIds() { return navigationItemIds; }
    public void setNavigationItemIds(List<String> navigationItemIds) { this.navigationItemIds = navigationItemIds; }
}
