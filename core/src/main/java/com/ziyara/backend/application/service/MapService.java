package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MapService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ProviderMapPinResponse> getProviderPins(List<String> types) {
        return serviceRepository.findActiveWithCoordinates(types)
                .stream().map(this::toPin).toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderMapPinResponse> getPortalPins(UUID providerId) {
        return serviceRepository.findByProviderIdWithCoordinates(providerId)
                .stream().map(this::toPin).toList();
    }

    private ProviderMapPinResponse toPin(Service s) {
        Double lat = s.getLatitude() != null ? s.getLatitude().doubleValue() : null;
        Double lon = s.getLongitude() != null ? s.getLongitude().doubleValue() : null;
        return ProviderMapPinResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .type(s.getType() != null ? s.getType().name() : null)
                .latitude(lat)
                .longitude(lon)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .thumbnailUrl(null)
                .build();
    }
}
