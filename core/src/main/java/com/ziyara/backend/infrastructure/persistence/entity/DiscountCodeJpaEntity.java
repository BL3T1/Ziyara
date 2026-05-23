package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.DiscountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity: DiscountCodeJpaEntity
 * Maps to 'discount_codes' table
 */
@Entity
@Table(name = "disc_discount_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCodeJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "description_ar", columnDefinition = "TEXT")
    private String descriptionAr;
    
    @Column(name = "type", nullable = false, length = 20)
    private String type; // PERCENTAGE, FIXED_AMOUNT
    
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;
    
    @Column(name = "min_booking_amount", precision = 12, scale = 2)
    private BigDecimal minBookingAmount = BigDecimal.ZERO;
    
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "usage_limit")
    private Integer usageLimit;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DiscountStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "sponsor", nullable = false, length = 20)
    private String sponsor = "COMPANY";

    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "company_share_pct", precision = 5, scale = 2)
    private BigDecimal companySharePct = new BigDecimal("100.00");

    @Column(name = "provider_share_pct", precision = 5, scale = 2)
    private BigDecimal providerSharePct = new BigDecimal("0.00");

    @Column(name = "approval_status", length = 30, nullable = false)
    private String approvalStatus = "DRAFT";

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_service_ids", columnDefinition = "jsonb")
    private List<String> applicableServiceIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_menu_section_ids", columnDefinition = "jsonb")
    private List<String> applicableMenuSectionIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_menu_item_ids", columnDefinition = "jsonb")
    private List<String> applicableMenuItemIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_room_type_ids", columnDefinition = "jsonb")
    private List<String> applicableRoomTypeIds;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = DiscountStatus.PENDING_APPROVAL;
        }
        if (usageCount == null) {
            usageCount = 0;
        }
        if (sponsor == null || sponsor.isBlank()) {
            sponsor = "COMPANY";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
