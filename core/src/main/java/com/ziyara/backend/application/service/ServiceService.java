package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.ServiceAvailabilityResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.modules.service.api.ServiceServiceApi;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.usecase.service.PublishServiceUseCase;
import com.ziyara.backend.domain.usecase.service.SuspendServiceUseCase;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service: ServiceService (Phase 2 â€“ Commands)
 * Handles service create, update, delete.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService implements ServiceServiceApi {

    private static final Set<BookingStatus> NON_OCCUPYING_STATUSES = Set.of(
            BookingStatus.CANCELLED, BookingStatus.REFUNDED,
            BookingStatus.EXPIRED,   BookingStatus.CLOSED);

    private final ServiceRepository serviceRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        if (request.getProviderId() == null) {
            throw new IllegalArgumentException("providerId is required");
        }
        if (!serviceProviderRepository.findById(request.getProviderId()).isPresent()) {
            throw new IllegalArgumentException("Provider not found: " + request.getProviderId());
        }
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setProviderId(request.getProviderId());
        svc.setType(request.getType());
        svc.setName(request.getName());
        svc.setDescription(request.getDescription());
        svc.setCity(request.getCity());
        svc.setCountry(request.getCountry());
        svc.setAddress(request.getAddress());
        svc.setBasePrice(request.getBasePrice());
        svc.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        svc.setMaxGuests(request.getMaxGuests() != null ? request.getMaxGuests() : 1);
        svc.setTotalRooms(request.getTotalRooms());
        svc.setAvailableRooms(request.getAvailableRooms());
        svc.setStarRating(request.getStarRating());
        svc.setAttributes(request.getAttributes());
        svc.setAmenities(request.getAmenities());
        if (request.getCheckInTime() != null) svc.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) svc.setCheckOutTime(request.getCheckOutTime());
        if (request.getLatitude() != null) svc.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) svc.setLongitude(request.getLongitude());
        if (request.getPolicies() != null) svc.setPolicies(request.getPolicies());
        com.ziyara.backend.domain.entity.Service saved = serviceRepository.save(svc);
        log.info("Service created: {} for provider {}", saved.getId(), request.getProviderId());
        return toResponse(saved);
    }

    @Transactional
    public ServiceResponse update(UUID id, UpdateServiceRequest request) {
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (request.getName() != null) svc.setName(request.getName());
        if (request.getDescription() != null) svc.setDescription(request.getDescription());
        if (request.getCity() != null) svc.setCity(request.getCity());
        if (request.getCountry() != null) svc.setCountry(request.getCountry());
        if (request.getAddress() != null) svc.setAddress(request.getAddress());
        if (request.getBasePrice() != null) svc.setBasePrice(request.getBasePrice());
        if (request.getStatus() != null) svc.setStatus(request.getStatus());
        if (request.getMaxGuests() != null) svc.setMaxGuests(request.getMaxGuests());
        if (request.getTotalRooms() != null) svc.setTotalRooms(request.getTotalRooms());
        if (request.getAvailableRooms() != null) svc.setAvailableRooms(request.getAvailableRooms());
        if (request.getStarRating() != null) svc.setStarRating(request.getStarRating());
        if (request.getAttributes() != null) svc.setAttributes(request.getAttributes());
        if (request.getAmenities() != null) svc.setAmenities(request.getAmenities());
        if (request.getCheckInTime() != null) svc.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) svc.setCheckOutTime(request.getCheckOutTime());
        if (request.getLatitude() != null) svc.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) svc.setLongitude(request.getLongitude());
        if (request.getPolicies() != null) svc.setPolicies(request.getPolicies());
        return toResponse(serviceRepository.save(svc));
    }

    /**
     * Checks room availability for a service.
     * Moved here from {@code ServiceController} to keep domain-repo access in the application layer.
     *
     * @param serviceId the service UUID
     * @param date      check-in date (nullable — returns capacity-based availability when null)
     * @param nights    number of nights (used only when date is non-null)
     * @return availability result with a human-readable message when unavailable
     */
    public ServiceAvailabilityResponse checkAvailability(UUID serviceId, LocalDate date, int nights) {
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        boolean available;
        String message = null;
        if (date != null && nights > 0) {
            LocalDate checkOut = date.plusDays(nights);
            List<Booking> overlapping = bookingRepository.findOverlappingBookings(serviceId, date, checkOut);
            int occupiedRooms = overlapping.stream()
                    .filter(b -> !NON_OCCUPYING_STATUSES.contains(b.getStatus()))
                    .mapToInt(Booking::getRooms)
                    .sum();
            Integer totalRooms = service.getTotalRooms();
            if (totalRooms != null && totalRooms > 0) {
                available = (totalRooms - occupiedRooms) >= 1;
                if (!available) message = "No rooms available for the selected dates";
            } else {
                available = occupiedRooms == 0;
                if (!available) message = "Fully booked for the selected dates";
            }
        } else {
            Integer avail = service.getAvailableRooms();
            available = (avail != null && avail > 0) || service.getTotalRooms() == null;
            if (!available) message = "No availability";
        }
        return ServiceAvailabilityResponse.builder()
                .available(available)
                .message(message)
                .build();
    }

    @Transactional
    public ServiceResponse approve(UUID id) {
        var result = new PublishServiceUseCase(serviceRepository)
                .execute(new PublishServiceUseCase.Input(id, null));
        if (!result.success()) throw new BusinessException(result.error());
        return toResponse(result.service());
    }

    @Transactional
    public ServiceResponse suspend(UUID id) {
        var result = new SuspendServiceUseCase(serviceRepository)
                .execute(new SuspendServiceUseCase.Input(id, null, null));
        if (!result.success()) throw new BusinessException(result.error());
        return toResponse(result.service());
    }

    @Transactional
    public void deleteById(UUID id) {
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        svc.softDelete();
        serviceRepository.save(svc);
    }

    private static ServiceResponse toResponse(com.ziyara.backend.domain.entity.Service s) {
        return ServiceResponse.builder()
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
                .build();
    }
}
