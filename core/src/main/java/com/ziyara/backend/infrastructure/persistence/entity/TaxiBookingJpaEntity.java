package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: TaxiBookingJpaEntity
 * Maps to 'taxi_bookings' table
 */
@Entity
@Table(name = "bkg_taxi_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxiBookingJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    
    @Column(name = "driver_id")
    private UUID driverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;
    
    @Column(name = "pickup_location", columnDefinition = "TEXT")
    private String pickupLocation;
    
    @Column(name = "destination_location", columnDefinition = "TEXT")
    private String destinationLocation;
    
    @Column(name = "pickup_latitude")
    private Double pickupLatitude;
    
    @Column(name = "pickup_longitude")
    private Double pickupLongitude;
    
    @Column(name = "destination_latitude")
    private Double destinationLatitude;
    
    @Column(name = "destination_longitude")
    private Double destinationLongitude;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "estimated_distance", precision = 8, scale = 2)
    private BigDecimal estimatedDistance;
    
    @Column(name = "actual_distance", precision = 8, scale = 2)
    private BigDecimal actualDistance;
    
    @Column(name = "estimated_price", precision = 12, scale = 2)
    private BigDecimal estimatedPrice;
    
    @Column(name = "actual_price", precision = 12, scale = 2)
    private BigDecimal actualPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaxiStatus status;
    
    @Column(name = "license_plate", length = 20)
    private String licensePlate;
    
    @Column(name = "driver_name")
    private String driverName;
    
    @Column(name = "vehicle_model")
    private String vehicleModel;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TaxiStatus.SEARCHING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
