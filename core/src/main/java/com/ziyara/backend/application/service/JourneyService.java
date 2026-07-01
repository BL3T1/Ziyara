package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.JourneyRecommendationResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JourneyService {

    private final ServiceRepository serviceRepository;

    public JourneyRecommendationResponse recommend(String city, int guests, BigDecimal maxBudget) {
        List<ServiceResponse> hotels = Stream.concat(
                serviceRepository.findByTypeAndCity(ServiceType.HOTEL, city).stream(),
                serviceRepository.findByTypeAndCity(ServiceType.RESORT, city).stream()
            )
            .filter(Service::isAvailable)
            .filter(s -> s.getAvailableRooms() == null || s.getAvailableRooms() >= 1)
            .filter(s -> s.getMaxGuests() == null || s.getMaxGuests() >= guests)
            .filter(s -> maxBudget == null || s.getBasePrice() == null || s.getBasePrice().compareTo(maxBudget) <= 0)
            .limit(5)
            .map(JourneyService::toResponse)
            .collect(Collectors.toList());

        List<ServiceResponse> taxis = serviceRepository.findByTypeAndStatus(ServiceType.TAXI, ServiceStatus.ACTIVE)
            .stream()
            .filter(s -> s.getDeletedAt() == null)
            .limit(5)
            .map(JourneyService::toResponse)
            .collect(Collectors.toList());

        List<ServiceResponse> restaurants = serviceRepository.findByTypeAndCity(ServiceType.RESTAURANT, city)
            .stream()
            .filter(Service::isAvailable)
            .filter(s -> maxBudget == null || s.getBasePrice() == null || s.getBasePrice().compareTo(maxBudget) <= 0)
            .limit(5)
            .map(JourneyService::toResponse)
            .collect(Collectors.toList());

        return JourneyRecommendationResponse.builder()
            .hotels(hotels)
            .taxis(taxis)
            .restaurants(restaurants)
            .build();
    }

    private static ServiceResponse toResponse(Service s) {
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
