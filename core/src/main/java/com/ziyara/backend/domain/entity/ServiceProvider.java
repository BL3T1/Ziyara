package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.ProviderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: ServiceProvider
 * Represents an organization or individual providing services
 */
public class ServiceProvider {
    
    private UUID id;
    private UUID userId; // Owner/Primary Contact
    private String name;
    private String nameAr;
    private String type; // e.g., Hotel, Taxi Company, Tour Agency
    private String registrationNumber;
    private String taxNumber;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    private String description;
    private String descriptionAr;
    private BigDecimal rating;
    private int reviewCount;
    /** Official classification assigned by admin (e.g. 3.0 = 3-star hotel). 0 = unset. */
    private BigDecimal globalRate;
    private ProviderStatus status;
    private boolean verified;
    /** Commission rate override (e.g. 10 = 10%). Null = use platform default 10%. */
    private BigDecimal commissionRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** User who approved activation (Super Admin / CEO workflow). */
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private Double latitude;
    private Double longitude;

    // Constructors
    public ServiceProvider() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ProviderStatus.PENDING_APPROVAL;
        this.rating = BigDecimal.ZERO;
        this.reviewCount = 0;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDescriptionAr() { return descriptionAr; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public BigDecimal getGlobalRate() { return globalRate; }
    public void setGlobalRate(BigDecimal globalRate) { this.globalRate = globalRate; }
    public ProviderStatus getStatus() { return status; }
    public void setStatus(ProviderStatus status) { this.status = status; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
