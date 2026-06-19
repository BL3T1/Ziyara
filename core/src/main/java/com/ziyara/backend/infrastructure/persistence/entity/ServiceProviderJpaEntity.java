package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.ProviderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: ServiceProviderJpaEntity
 * Maps to 'service_providers' table
 */
@Entity
@Table(name = "hotel_service_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceProviderJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_name_ar", length = 255)
    private String companyNameAr;
    
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;
    
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;
    
    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_ar", columnDefinition = "TEXT")
    private String descriptionAr;

    @Column(name = "provider_type", length = 64)
    private String providerType;

    @Column(name = "registration_number", length = 128)
    private String registrationNumber;
    
    /** NUMERIC(3,2): exact rating 0.00–5.00 (V16 type change from DOUBLE PRECISION). */
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    /** NUMERIC(3,1): official classification (e.g. 3.0 = 3-star). 0 = unset. V30. */
    @Column(name = "global_rate", precision = 3, scale = 1, nullable = false)
    private BigDecimal globalRate = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProviderStatus status;
    
    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ProviderStatus.PENDING_APPROVAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
