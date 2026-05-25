package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.enums.VehicleType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;
import com.ziyara.backend.domain.usecase.taxi.CreateTaxiBookingUseCase;
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
        var result = new CreateTaxiBookingUseCase(bookingRepository, taxiBookingRepository).execute(
                new CreateTaxiBookingUseCase.Input(
                        bookingId,
                        request.getVehicleType() != null ? request.getVehicleType() : VehicleType.STANDARD,
                        request.getPickupLocation(), request.getDestinationLocation(),
                        request.getPickupLatitude() != null ? request.getPickupLatitude() : 0.0,
                        request.getPickupLongitude() != null ? request.getPickupLongitude() : 0.0,
                        request.getDestinationLatitude() != null ? request.getDestinationLatitude() : 0.0,
                        request.getDestinationLongitude() != null ? request.getDestinationLongitude() : 0.0,
                        request.getScheduledAt(),
                        request.getEstimatedDistance() != null ? request.getEstimatedDistance() : java.math.BigDecimal.ZERO,
                        request.getEstimatedPrice()));
        if (!result.success()) throw new BusinessException(result.error());
        return mapToResponse(result.taxiBooking());
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
    
    /**
     * Verifies that {@code userId} is the assigned driver for the taxi linked to
     * {@code bookingId} (the parent booking ID, not the taxi booking ID).
     * Throws {@link UnauthorizedException} if not.
     */
    @Transactional(readOnly = true)
    public void assertIsDriver(UUID bookingId, UUID userId) {
        TaxiBooking tb = taxiBookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi booking not found for booking " + bookingId));
        if (!userId.equals(tb.getDriverId())) {
            throw new UnauthorizedException("Not the assigned driver for booking " + bookingId);
        }
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
