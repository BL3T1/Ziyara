package com.ziyarah.domain.entity;

import com.ziyarah.domain.enums.ServiceStatus;
import com.ziyarah.domain.enums.ServiceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain Entity: Service
 * Bookable services offered by providers
 * No framework dependencies - pure Java
 */
public class Service {
    
    private UUID id;
    private UUID providerId;
    private ServiceType type;
    private String name;
    private String description;
    private String location;
    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal basePrice;
    private String currency;
    private ServiceStatus status;
    private Map<String, Object> attributes;
    private Map<String, Object> amenities;
    private String policies;
    private Integer starRating;
    private Integer totalRooms;
    private Integer availableRooms;
    private Integer maxGuests;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // Domain behavior
    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE && deletedAt == null;
    }

    public boolean hasRoomsAvailable(int requestedRooms) {
        if (availableRooms == null) return true;
        return availableRooms >= requestedRooms;
    }

    public void reduceAvailability(int rooms) {
        if (availableRooms != null) {
            this.availableRooms = Math.max(0, availableRooms - rooms);
        }
    }

    public void increaseAvailability(int rooms) {
        if (availableRooms != null && totalRooms != null) {
            this.availableRooms = Math.min(totalRooms, availableRooms + rooms);
        }
    }

    public boolean supportsTaxiAddOn() {
        return type.supportsTaxiAddOn();
    }

    public void activate() {
        this.status = ServiceStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ServiceStatus.INACTIVE;
    }

    public void suspend() {
        this.status = ServiceStatus.SUSPENDED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    // Constructors
    public Service() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ServiceStatus.PENDING_APPROVAL;
        this.currency = "USD";
        this.maxGuests = 1;
        this.checkInTime = LocalTime.of(14, 0);
        this.checkOutTime = LocalTime.of(11, 0);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public ServiceType getType() { return type; }
    public void setType(ServiceType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public Map<String, Object> getAmenities() { return amenities; }
    public void setAmenities(Map<String, Object> amenities) { this.amenities = amenities; }
    public String getPolicies() { return policies; }
    public void setPolicies(String policies) { this.policies = policies; }
    public Integer getStarRating() { return starRating; }
    public void setStarRating(Integer starRating) { this.starRating = starRating; }
    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }
    public Integer getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(Integer availableRooms) { this.availableRooms = availableRooms; }
    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
