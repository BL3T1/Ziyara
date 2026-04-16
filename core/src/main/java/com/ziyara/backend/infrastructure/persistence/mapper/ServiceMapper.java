package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ServiceMapper - ServiceJpaEntity <-> Service (domain)
 */
@Component
public class ServiceMapper {

    public Service toDomainEntity(ServiceJpaEntity entity) {
        if (entity == null) return null;
        Service s = new Service();
        s.setId(entity.getId());
        s.setProviderId(entity.getProviderId());
        s.setType(entity.getType());
        s.setName(entity.getName());
        s.setDescription(entity.getDescription());
        s.setLocation(entity.getLocation());
        s.setAddress(entity.getAddress());
        s.setCity(entity.getCity());
        s.setCountry(entity.getCountry());
        s.setLatitude(entity.getLatitude());
        s.setLongitude(entity.getLongitude());
        s.setBasePrice(entity.getBasePrice());
        s.setCurrency(entity.getCurrency() != null ? entity.getCurrency() : "USD");
        s.setStatus(entity.getStatus());
        s.setAttributes(entity.getAttributes());
        s.setAmenities(entity.getAmenities());
        s.setPolicies(entity.getPolicies());
        s.setStarRating(entity.getStarRating());
        s.setTotalRooms(entity.getTotalRooms());
        s.setAvailableRooms(entity.getAvailableRooms());
        s.setMaxGuests(entity.getMaxGuests() != null ? entity.getMaxGuests() : 1);
        s.setSeasonalMultiplier(entity.getSeasonalMultiplier() != null ? entity.getSeasonalMultiplier() : java.math.BigDecimal.ONE);
        s.setTaxRate(entity.getTaxRate() != null ? entity.getTaxRate() : java.math.BigDecimal.ZERO);
        s.setCheckInTime(entity.getCheckInTime());
        s.setCheckOutTime(entity.getCheckOutTime());
        s.setCreatedAt(entity.getCreatedAt());
        s.setUpdatedAt(entity.getUpdatedAt());
        s.setDeletedAt(entity.getDeletedAt());
        return s;
    }

    public ServiceJpaEntity toJpaEntity(Service s) {
        if (s == null) return null;
        return ServiceJpaEntity.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .type(s.getType())
                .name(s.getName())
                .description(s.getDescription())
                .location(s.getLocation())
                .address(s.getAddress())
                .city(s.getCity())
                .country(s.getCountry())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .basePrice(s.getBasePrice())
                .currency(s.getCurrency())
                .status(s.getStatus())
                .attributes(s.getAttributes())
                .amenities(s.getAmenities())
                .policies(s.getPolicies())
                .starRating(s.getStarRating())
                .totalRooms(s.getTotalRooms())
                .availableRooms(s.getAvailableRooms())
                .maxGuests(s.getMaxGuests())
                .seasonalMultiplier(s.getSeasonalMultiplier())
                .taxRate(s.getTaxRate())
                .checkInTime(s.getCheckInTime())
                .checkOutTime(s.getCheckOutTime())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .deletedAt(s.getDeletedAt())
                .build();
    }
}
