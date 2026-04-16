package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity: ServiceJpaEntity
 * Infrastructure layer representation of Service
 * Maps to 'services' table in PostgreSQL
 */
@Entity
@Table(name = "hotel_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE services SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ServiceJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ServiceType type;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceStatus status;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> amenities = new HashMap<>();
    
    @Column(name = "policies", columnDefinition = "TEXT")
    private String policies;
    
    @Column(name = "star_rating")
    private Integer starRating;
    
    @Column(name = "total_rooms")
    private Integer totalRooms;
    
    @Column(name = "available_rooms")
    private Integer availableRooms;
    
    @Column(name = "max_guests")
    private Integer maxGuests = 1;
    
    @Column(name = "seasonal_multiplier", precision = 5, scale = 2)
    private BigDecimal seasonalMultiplier = BigDecimal.ONE;
    
    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    @Column(name = "check_in_time")
    private LocalTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ServiceStatus.PENDING_APPROVAL;
        }
        if (checkInTime == null) {
            checkInTime = LocalTime.of(14, 0);
        }
        if (checkOutTime == null) {
            checkOutTime = LocalTime.of(11, 0);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
