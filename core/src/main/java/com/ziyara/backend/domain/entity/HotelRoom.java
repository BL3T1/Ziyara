package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.common.Attributes;
import com.ziyara.backend.domain.enums.HotelRoomStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class HotelRoom {

    private UUID id;
    private UUID serviceId;
    private String roomType;
    private String roomName;
    private String description;
    private Integer capacity;
    private BigDecimal basePrice;
    private String currency;
    private Integer quantityTotal;
    private Integer quantityAvailable;
    private Attributes amenities = Attributes.empty();
    private HotelRoomStatus status;
    private Integer sortOrder;
    private Integer floorNumber;
    private String roomCategory = "STANDARD";
    private String bedType;
    private BigDecimal areaSqm;
    private String viewType;
    private boolean smokingAllowed = false;
    private boolean isAccessible = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior
    public void activate() {
        this.status = HotelRoomStatus.ACTIVE;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void deactivate() {
        this.status = HotelRoomStatus.INACTIVE;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Room price must be non-negative");
        }
        this.basePrice = newPrice;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public boolean hasCapacity(int requestedGuests) {
        return capacity != null && requestedGuests <= capacity;
    }

    public boolean isAvailable() {
        return status == HotelRoomStatus.ACTIVE
                && (quantityAvailable == null || quantityAvailable > 0);
    }

    public HotelRoom() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(Integer quantityTotal) { this.quantityTotal = quantityTotal; }

    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public Attributes getTypedAmenities() { return amenities; }
    public Map<String, Object> getAmenities() { return amenities == null ? null : amenities.toMap(); }
    public void setAmenities(Map<String, Object> amenities) { this.amenities = amenities == null ? Attributes.empty() : Attributes.of(amenities); }

    public HotelRoomStatus getStatus() { return status; }
    public void setStatus(HotelRoomStatus status) { this.status = status; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }

    public String getRoomCategory() { return roomCategory; }
    public void setRoomCategory(String roomCategory) { this.roomCategory = roomCategory; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }

    public BigDecimal getAreaSqm() { return areaSqm; }
    public void setAreaSqm(BigDecimal areaSqm) { this.areaSqm = areaSqm; }

    public String getViewType() { return viewType; }
    public void setViewType(String viewType) { this.viewType = viewType; }

    public boolean isSmokingAllowed() { return smokingAllowed; }
    public void setSmokingAllowed(boolean smokingAllowed) { this.smokingAllowed = smokingAllowed; }

    public boolean isAccessible() { return isAccessible; }
    public void setAccessible(boolean accessible) { isAccessible = accessible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
