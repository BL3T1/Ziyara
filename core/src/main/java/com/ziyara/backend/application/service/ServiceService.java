package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service: ServiceService (Phase 2 – Commands)
 * Handles service create, update, delete.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
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
        return toResponse(serviceRepository.save(svc));
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
