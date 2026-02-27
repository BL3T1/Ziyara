package com.ziyarah.application.service;

import com.ziyarah.application.dto.response.TaxiBookingResponse;
import com.ziyarah.domain.entity.TaxiBooking;
import com.ziyarah.domain.enums.TaxiStatus;
import com.ziyarah.domain.repository.TaxiBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: TaxiBookingService
 * Handles taxi-specific booking lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxiBookingService {
    
    private final TaxiBookingRepository taxiBookingRepository;
    
    @Transactional
    public TaxiBookingResponse updateTaxiStatus(UUID taxiBookingId, TaxiStatus status) {
        log.info("Updating taxi booking {} to status {}", taxiBookingId, status);
        
        TaxiBooking booking = taxiBookingRepository.findById(taxiBookingId)
                .orElseThrow(() -> new RuntimeException("Taxi booking not found"));
        
        booking.setStatus(status);
        if (status == TaxiStatus.IN_PROGRESS) booking.setStartedAt(java.time.LocalDateTime.now());
        if (status == TaxiStatus.COMPLETED) booking.setCompletedAt(java.time.LocalDateTime.now());
        
        return mapToResponse(taxiBookingRepository.save(booking));
    }
    
    @Transactional
    public TaxiBookingResponse assignDriver(UUID taxiBookingId, UUID driverId, String driverName, String plate, String model) {
        log.info("Assigning driver {} to taxi booking {}", driverId, taxiBookingId);
        
        TaxiBooking booking = taxiBookingRepository.findById(taxiBookingId)
                .orElseThrow(() -> new RuntimeException("Taxi booking not found"));
        
        booking.setDriverId(driverId);
        booking.setDriverName(driverName);
        booking.setLicensePlate(plate);
        booking.setVehicleModel(model);
        booking.setStatus(TaxiStatus.ASSIGNED);
        
        return mapToResponse(taxiBookingRepository.save(booking));
    }
    
    @Transactional(readOnly = true)
    public List<TaxiBookingResponse> getActiveBookings() {
        return taxiBookingRepository.findByStatus(TaxiStatus.ASSIGNED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private TaxiBookingResponse mapToResponse(TaxiBooking booking) {
        return TaxiBookingResponse.builder()
                .id(booking.getId())
                .bookingId(booking.getBookingId())
                .driverId(booking.getDriverId())
                .vehicleType(booking.getVehicleType())
                .pickupLocation(booking.getPickupLocation())
                .destinationLocation(booking.getDestinationLocation())
                .scheduledAt(booking.getScheduledAt())
                .startedAt(booking.getStartedAt())
                .completedAt(booking.getCompletedAt())
                .status(booking.getStatus())
                .licensePlate(booking.getLicensePlate())
                .driverName(booking.getDriverName())
                .build();
    }
}
