package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.TaxiBooking;
import com.ziyarah.infrastructure.persistence.entity.TaxiBookingJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: TaxiBookingMapper
 */
@Component
public class TaxiBookingMapper {
    
    public TaxiBooking toDomainEntity(TaxiBookingJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        TaxiBooking taxiBooking = new TaxiBooking();
        taxiBooking.setId(entity.getId());
        taxiBooking.setBookingId(entity.getBookingId());
        taxiBooking.setDriverId(entity.getDriverId());
        taxiBooking.setVehicleType(entity.getVehicleType());
        taxiBooking.setPickupLocation(entity.getPickupLocation());
        taxiBooking.setDestinationLocation(entity.getDestinationLocation());
        taxiBooking.setPickupLatitude(entity.getPickupLatitude() != null ? entity.getPickupLatitude() : 0.0);
        taxiBooking.setPickupLongitude(entity.getPickupLongitude() != null ? entity.getPickupLongitude() : 0.0);
        taxiBooking.setDestinationLatitude(entity.getDestinationLatitude() != null ? entity.getDestinationLatitude() : 0.0);
        taxiBooking.setDestinationLongitude(entity.getDestinationLongitude() != null ? entity.getDestinationLongitude() : 0.0);
        taxiBooking.setScheduledAt(entity.getScheduledAt());
        taxiBooking.setStartedAt(entity.getStartedAt());
        taxiBooking.setCompletedAt(entity.getCompletedAt());
        taxiBooking.setEstimatedDistance(entity.getEstimatedDistance());
        taxiBooking.setActualDistance(entity.getActualDistance());
        taxiBooking.setEstimatedPrice(entity.getEstimatedPrice());
        taxiBooking.setActualPrice(entity.getActualPrice());
        taxiBooking.setStatus(entity.getStatus());
        taxiBooking.setLicensePlate(entity.getLicensePlate());
        taxiBooking.setDriverName(entity.getDriverName());
        taxiBooking.setVehicleModel(entity.getVehicleModel());
        taxiBooking.setCreatedAt(entity.getCreatedAt());
        taxiBooking.setUpdatedAt(entity.getUpdatedAt());
        
        return taxiBooking;
    }
    
    public TaxiBookingJpaEntity toJpaEntity(TaxiBooking taxiBooking) {
        if (taxiBooking == null) {
            return null;
        }
        
        return TaxiBookingJpaEntity.builder()
                .id(taxiBooking.getId())
                .bookingId(taxiBooking.getBookingId())
                .driverId(taxiBooking.getDriverId())
                .vehicleType(taxiBooking.getVehicleType())
                .pickupLocation(taxiBooking.getPickupLocation())
                .destinationLocation(taxiBooking.getDestinationLocation())
                .pickupLatitude(taxiBooking.getPickupLatitude())
                .pickupLongitude(taxiBooking.getPickupLongitude())
                .destinationLatitude(taxiBooking.getDestinationLatitude())
                .destinationLongitude(taxiBooking.getDestinationLongitude())
                .scheduledAt(taxiBooking.getScheduledAt())
                .startedAt(taxiBooking.getStartedAt())
                .completedAt(taxiBooking.getCompletedAt())
                .estimatedDistance(taxiBooking.getEstimatedDistance())
                .actualDistance(taxiBooking.getActualDistance())
                .estimatedPrice(taxiBooking.getEstimatedPrice())
                .actualPrice(taxiBooking.getActualPrice())
                .status(taxiBooking.getStatus())
                .licensePlate(taxiBooking.getLicensePlate())
                .driverName(taxiBooking.getDriverName())
                .vehicleModel(taxiBooking.getVehicleModel())
                .createdAt(taxiBooking.getCreatedAt())
                .updatedAt(taxiBooking.getUpdatedAt())
                .build();
    }
}
