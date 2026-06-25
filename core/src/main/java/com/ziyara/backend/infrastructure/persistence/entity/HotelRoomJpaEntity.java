package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.HotelRoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "hotel_service_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelRoomJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "room_type", nullable = false, length = 64)
    private String roomType;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal = 0;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> amenities = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private HotelRoomStatus status = HotelRoomStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "room_category", nullable = false, length = 32)
    private String roomCategory = "STANDARD";

    @Column(name = "bed_type", length = 16)
    private String bedType;

    @Column(name = "area_sqm", precision = 5, scale = 1)
    private BigDecimal areaSqm;

    @Column(name = "view_type", length = 16)
    private String viewType;

    @Column(name = "smoking_allowed", nullable = false)
    private boolean smokingAllowed = false;

    @Column(name = "is_accessible", nullable = false)
    private boolean isAccessible = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
