package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.ServiceImageCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: ServiceImageJpaEntity
 * Maps to 'service_images' table
 */
@Entity
@Table(name = "hotel_service_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceImageJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;
    
    @Column(name = "url", nullable = false, length = 500)
    private String url;
    
    @Column(name = "alt_text")
    private String altText;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private ServiceImageCategory category = ServiceImageCategory.PROPERTY;

    @Column(name = "context_key", length = 100)
    private String contextKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
