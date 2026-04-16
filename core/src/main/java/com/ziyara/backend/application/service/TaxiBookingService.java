package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.enums.VehicleType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
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
    private final BookingRepository bookingRepository;

    /** Phase 3: Create taxi add-on for a booking. */
    @Transactional
    public TaxiBookingResponse createForBooking(UUID bookingId, AddTaxiRequest request) {
        if (!bookingRepository.findById(bookingId).isPresent()) {
            throw new RuntimeException("Booking not found");
        }
        if (taxiBookingRepository.findByBookingId(bookingId).isPresent()) {
            throw new RuntimeException("Taxi already linked to this booking");
        }
        TaxiBooking taxi = new TaxiBooking();
        taxi.setBookingId(bookingId);
        taxi.setPickupLocation(request.getPickupLocation());
        taxi.setDestinationLocation(request.getDestinationLocation());
        taxi.setVehicleType(request.getVehicleType() != null ? request.getVehicleType() : VehicleType.STANDARD);
        taxi.setScheduledAt(request.getScheduledAt());
        taxi.setStatus(TaxiStatus.SEARCHING);
        return mapToResponse(taxiBookingRepository.save(taxi));
    }

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

    @Transactional(readOnly = true)
    public TaxiBookingResponse getTaxiBooking(UUID id) {
        return taxiBookingRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi booking not found"));
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
