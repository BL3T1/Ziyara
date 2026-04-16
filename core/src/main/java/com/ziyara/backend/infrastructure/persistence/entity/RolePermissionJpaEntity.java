package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sys_role_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "role_id", updatable = false, nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", updatable = false, nullable = false)
    private UUID permissionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static RolePermissionJpaEntityBuilder builder() {
        return new RolePermissionJpaEntityBuilder();
    }

    public static class RolePermissionJpaEntityBuilder {
        private UUID id;
        private UUID roleId;
        private UUID permissionId;
        private LocalDateTime createdAt;

        public RolePermissionJpaEntityBuilder id(UUID id) { this.id = id; return this; }
        public RolePermissionJpaEntityBuilder roleId(UUID roleId) { this.roleId = roleId; return this; }
        public RolePermissionJpaEntityBuilder permissionId(UUID permissionId) { this.permissionId = permissionId; return this; }
        public RolePermissionJpaEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RolePermissionJpaEntity build() {
            RolePermissionJpaEntity e = new RolePermissionJpaEntity();
            e.setId(id); e.setRoleId(roleId); e.setPermissionId(permissionId);
            e.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
            return e;
        }
    }
}
