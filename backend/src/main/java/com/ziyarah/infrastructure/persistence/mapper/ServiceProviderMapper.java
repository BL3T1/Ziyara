package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.ServiceProvider;
import com.ziyarah.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ServiceProviderMapper
 */
@Component
public class ServiceProviderMapper {
    
    public ServiceProvider toDomainEntity(ServiceProviderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ServiceProvider provider = new ServiceProvider();
        provider.setId(entity.getId());
        provider.setUserId(entity.getUserId());
        provider.setName(entity.getName());
        provider.setType(entity.getType());
        provider.setRegistrationNumber(entity.getRegistrationNumber());
        provider.setTaxNumber(entity.getTaxNumber());
        provider.setAddress(entity.getAddress());
        provider.setPhone(entity.getPhone());
        provider.setEmail(entity.getEmail());
        provider.setWebsite(entity.getWebsite());
        provider.setLogoUrl(entity.getLogoUrl());
        provider.setDescription(entity.getDescription());
        provider.setRating(entity.getRating() != null ? entity.getRating() : 0.0);
        provider.setReviewCount(entity.getReviewCount() != null ? entity.getReviewCount() : 0);
        provider.setStatus(entity.getStatus());
        provider.setVerified(entity.getVerified() != null && entity.getVerified());
        provider.setCreatedAt(entity.getCreatedAt());
        provider.setUpdatedAt(entity.getUpdatedAt());
        
        return provider;
    }
    
    public ServiceProviderJpaEntity toJpaEntity(ServiceProvider provider) {
        if (provider == null) {
            return null;
        }
        
        return ServiceProviderJpaEntity.builder()
                .id(provider.getId())
                .userId(provider.getUserId())
                .name(provider.getName())
                .type(provider.getType())
                .registrationNumber(provider.getRegistrationNumber())
                .taxNumber(provider.getTaxNumber())
                .address(provider.getAddress())
                .phone(provider.getPhone())
                .email(provider.getEmail())
                .website(provider.getWebsite())
                .logoUrl(provider.getLogoUrl())
                .description(provider.getDescription())
                .rating(provider.getRating())
                .reviewCount(provider.getReviewCount())
                .status(provider.getStatus())
                .verified(provider.isVerified())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}
