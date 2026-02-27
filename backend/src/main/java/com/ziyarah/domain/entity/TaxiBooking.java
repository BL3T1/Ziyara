package com.ziyarah.domain.entity;

import com.ziyarah.domain.enums.TaxiStatus;
import com.ziyarah.domain.enums.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: TaxiBooking
 * Represents a specialized taxi or transport reservation
 */
public class TaxiBooking {
    
    private UUID id;
    private UUID bookingId; // Reference to core booking
    private UUID driverId;
    private VehicleType vehicleType;
    private String pickupLocation;
    private String destinationLocation;
    private double pickupLatitude;
    private double pickupLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private BigDecimal estimatedDistance; // in km
    private BigDecimal actualDistance;
    private BigDecimal estimatedPrice;
    private BigDecimal actualPrice;
    private TaxiStatus status;
    private String licensePlate;
    private String driverName;
    private String vehicleModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior methods
    public boolean canStart() {
        return status == TaxiStatus.ARRIVED_AT_PICKUP || status == TaxiStatus.ASSIGNED;
    }

    public void start() {
        if (canStart()) {
            this.status = TaxiStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete(BigDecimal actualPrice, BigDecimal actualDistance) {
        this.status = TaxiStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.actualPrice = actualPrice;
        this.actualDistance = actualDistance;
    }

    // Constructors
    public TaxiBooking() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TaxiStatus.SEARCHING;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }
    public double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public BigDecimal getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(BigDecimal estimatedDistance) { this.estimatedDistance = estimatedDistance; }
    public BigDecimal getActualDistance() { return actualDistance; }
    public void setActualDistance(BigDecimal actualDistance) { this.actualDistance = actualDistance; }
    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }
    public BigDecimal getActualPrice() { return actualPrice; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    public TaxiStatus getStatus() { return status; }
    public void setStatus(TaxiStatus status) { this.status = status; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
