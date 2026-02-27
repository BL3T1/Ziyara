package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateServiceProviderRequest;
import com.ziyarah.application.dto.request.UpdateServiceProviderRequest;
import com.ziyarah.application.dto.response.ServiceProviderResponse;
import com.ziyarah.domain.entity.ServiceProvider;
import com.ziyarah.domain.enums.ProviderStatus;
import com.ziyarah.domain.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: ServiceProviderService
 * Handles service provider management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderService {
    
    private final ServiceProviderRepository serviceProviderRepository;
    
    @Transactional
    public ServiceProviderResponse createProvider(CreateServiceProviderRequest request) {
        log.info("Creating service provider: {}", request.getName());
        
        if (serviceProviderRepository.existsByName(request.getName())) {
            throw new RuntimeException("Provider with this name already exists");
        }
        
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(request.getUserId());
        provider.setName(request.getName());
        provider.setType(request.getType());
        provider.setRegistrationNumber(request.getRegistrationNumber());
        provider.setPhone(request.getPhone());
        provider.setEmail(request.getEmail());
        provider.setAddress(request.getAddress());
        provider.setDescription(request.getDescription());
        provider.setStatus(ProviderStatus.PENDING_VERIFICATION);
        provider.setRating(0.0);
        provider.setReviewCount(0);
        provider.setVerified(false);
        
        return mapToResponse(serviceProviderRepository.save(provider));
    }
    
    @Transactional
    public ServiceProviderResponse updateProvider(UUID providerId, UpdateServiceProviderRequest request) {
        log.info("Updating service provider: {}", providerId);
        
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
        
        if (request.getName() != null) provider.setName(request.getName());
        if (request.getPhone() != null) provider.setPhone(request.getPhone());
        if (request.getEmail() != null) provider.setEmail(request.getEmail());
        if (request.getAddress() != null) provider.setAddress(request.getAddress());
        if (request.getDescription() != null) provider.setDescription(request.getDescription());
        if (request.getStatus() != null) provider.setStatus(request.getStatus());
        if (request.getWebsite() != null) provider.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) provider.setLogoUrl(request.getLogoUrl());
        if (request.getVerified() != null) provider.setVerified(request.getVerified());
        
        return mapToResponse(serviceProviderRepository.save(provider));
    }
    
    @Transactional(readOnly = true)
    public ServiceProviderResponse getProvider(UUID id) {
        return serviceProviderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
    }
    
    @Transactional(readOnly = true)
    public List<ServiceProviderResponse> getAllProviders() {
        return serviceProviderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ServiceProviderResponse mapToResponse(ServiceProvider provider) {
        return ServiceProviderResponse.builder()
                .id(provider.getId())
                .userId(provider.getUserId())
                .name(provider.getName())
                .type(provider.getType())
                .registrationNumber(provider.getRegistrationNumber())
                .phone(provider.getPhone())
                .email(provider.getEmail())
                .address(provider.getAddress())
                .rating(provider.getRating())
                .reviewCount(provider.getReviewCount())
                .status(provider.getStatus())
                .verified(provider.isVerified())
                .createdAt(provider.getCreatedAt())
                .build();
    }
}
