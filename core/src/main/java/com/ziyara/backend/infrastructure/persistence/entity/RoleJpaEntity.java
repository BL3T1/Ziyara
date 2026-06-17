package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sys_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "name_ar", length = 100)
    private String nameAr;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_ar", columnDefinition = "TEXT")
    private String descriptionAr;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private RoleLevel level = RoleLevel.EMPLOYEE;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "is_system_role", nullable = false)
    private Boolean systemRole = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoleStatus status = RoleStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** When set for a custom role, company dashboard sidebar shows only these item IDs (order preserved). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "navigation_item_ids", columnDefinition = "jsonb")
    private List<String> navigationItemIds;

    @Column(name = "max_discount_pct", nullable = false)
    private short maxDiscountPct;

    /** True if this role can be assigned to provider portal staff (independent of code prefix). */
    @Column(name = "is_provider_role", nullable = false)
    private boolean providerRole = false;

    /** Maximum single payout request amount for this role; null = no limit. */
    @Column(name = "max_payout_request_amount", precision = 15, scale = 2)
    private BigDecimal maxPayoutRequestAmount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
