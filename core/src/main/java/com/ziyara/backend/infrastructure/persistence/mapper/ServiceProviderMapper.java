package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
        provider.setName(entity.getCompanyName());
        provider.setNameAr(entity.getCompanyNameAr());
        provider.setAddress(entity.getAddress());
        provider.setPhone(entity.getContactPhone());
        provider.setEmail(entity.getContactEmail());
        provider.setWebsite(entity.getWebsite());
        provider.setLogoUrl(entity.getLogoUrl());
        provider.setDescription(entity.getDescription());
        provider.setDescriptionAr(entity.getDescriptionAr());
        provider.setType(entity.getProviderType());
        provider.setRegistrationNumber(entity.getRegistrationNumber());
        provider.setUserId(entity.getCreatedBy());
        provider.setRating(entity.getRating() != null ? entity.getRating() : BigDecimal.ZERO);
        provider.setReviewCount(entity.getReviewCount() != null ? entity.getReviewCount() : 0);
        provider.setGlobalRate(entity.getGlobalRate() != null ? entity.getGlobalRate() : BigDecimal.ZERO);
        provider.setStatus(entity.getStatus());
        provider.setVerified(entity.getVerified() != null && entity.getVerified());
        provider.setCommissionRate(entity.getCommissionRate());
        provider.setCreatedAt(entity.getCreatedAt());
        provider.setUpdatedAt(entity.getUpdatedAt());
        provider.setApprovedBy(entity.getApprovedBy());
        provider.setApprovedAt(entity.getApprovedAt());

        return provider;
    }
    
    public ServiceProviderJpaEntity toJpaEntity(ServiceProvider provider) {
        if (provider == null) {
            return null;
        }
        
        return ServiceProviderJpaEntity.builder()
                .id(provider.getId())
                .companyName(provider.getName())
                .companyNameAr(provider.getNameAr())
                .address(provider.getAddress())
                .contactPhone(provider.getPhone())
                .contactEmail(provider.getEmail())
                .website(provider.getWebsite())
                .logoUrl(provider.getLogoUrl())
                .description(provider.getDescription())
                .descriptionAr(provider.getDescriptionAr())
                .providerType(provider.getType())
                .registrationNumber(provider.getRegistrationNumber())
                .createdBy(provider.getUserId())
                .rating(provider.getRating())
                .reviewCount(provider.getReviewCount())
                .globalRate(provider.getGlobalRate() != null ? provider.getGlobalRate() : BigDecimal.ZERO)
                .status(provider.getStatus())
                .verified(provider.isVerified())
                .commissionRate(provider.getCommissionRate())
                .approvedBy(provider.getApprovedBy())
                .approvedAt(provider.getApprovedAt())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}
