package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.ProviderType;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ServiceProviderMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ServiceProviderJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ServiceProviderRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class ServiceProviderRepositoryAdapter implements ServiceProviderRepository {
    
    private final ServiceProviderJpaRepository serviceProviderJpaRepository;
    private final ServiceProviderMapper serviceProviderMapper;
    
    @Override
    public ServiceProvider save(ServiceProvider provider) {
        ServiceProviderJpaEntity entity = serviceProviderMapper.toJpaEntity(provider);
        ServiceProviderJpaEntity savedEntity = serviceProviderJpaRepository.save(entity);
        return serviceProviderMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<ServiceProvider> findById(UUID id) {
        return serviceProviderJpaRepository.findById(id)
                .map(serviceProviderMapper::toDomainEntity);
    }
    
    @Override
    public Optional<ServiceProvider> findByUserId(UUID userId) {
        return serviceProviderJpaRepository.findByCreatedBy(userId)
                .map(serviceProviderMapper::toDomainEntity);
    }
    
    @Override
    public Optional<ServiceProvider> findByName(String name) {
        return serviceProviderJpaRepository.findByCompanyName(name)
                .map(serviceProviderMapper::toDomainEntity);
    }
    
    @Override
    public List<ServiceProvider> findByStatus(ProviderStatus status) {
        return serviceProviderJpaRepository.findByStatus(status).stream()
                .map(serviceProviderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ServiceProvider> findByType(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        try {
            ProviderType pt = ProviderType.valueOf(type.trim().toUpperCase());
            return serviceProviderJpaRepository.findByProviderTypeAndDeletedAtIsNull(pt).stream()
                    .map(serviceProviderMapper::toDomainEntity)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public List<ServiceProvider> findAll() {
        return serviceProviderJpaRepository.findAll().stream()
                .map(serviceProviderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<ServiceProvider> findAll(PageQuery pageQuery) {
        return PageConverter.toPagedResult(serviceProviderJpaRepository.findByDeletedAtIsNull(PageConverter.toPageable(pageQuery)), serviceProviderMapper::toDomainEntity);
    }

    @Override
    public PagedResult<ServiceProvider> findByStatus(ProviderStatus status, PageQuery pageQuery) {
        return PageConverter.toPagedResult(serviceProviderJpaRepository.findByStatusAndDeletedAtIsNull(status, PageConverter.toPageable(pageQuery)), serviceProviderMapper::toDomainEntity);
    }

    @Override
    public PagedResult<ServiceProvider> findByProviderType(String providerType, PageQuery pageQuery) {
        try {
            ProviderType pt = ProviderType.valueOf(providerType.trim().toUpperCase());
            return PageConverter.toPagedResult(serviceProviderJpaRepository.findByProviderTypeAndDeletedAtIsNull(pt, PageConverter.toPageable(pageQuery)), serviceProviderMapper::toDomainEntity);
        } catch (IllegalArgumentException e) {
            return PageConverter.toPagedResult(org.springframework.data.domain.Page.empty(), serviceProviderMapper::toDomainEntity);
        }
    }

    @Override
    public PagedResult<ServiceProvider> findByStatusAndProviderType(ProviderStatus status, String providerType, PageQuery pageQuery) {
        try {
            ProviderType pt = ProviderType.valueOf(providerType.trim().toUpperCase());
            return PageConverter.toPagedResult(serviceProviderJpaRepository.findByStatusAndProviderTypeAndDeletedAtIsNull(status, pt, PageConverter.toPageable(pageQuery)), serviceProviderMapper::toDomainEntity);
        } catch (IllegalArgumentException e) {
            return PageConverter.toPagedResult(org.springframework.data.domain.Page.empty(), serviceProviderMapper::toDomainEntity);
        }
    }

    @Override
    public List<ServiceProvider> findByExpiryDate(LocalDate date) {
        return serviceProviderJpaRepository.findByExpiryDateAndNotDeleted(date).stream()
                .map(serviceProviderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return serviceProviderJpaRepository.count();
    }
    
    @Override
    public void deleteById(UUID id) {
        serviceProviderJpaRepository.deleteById(id);
    }

    @Override
    public void softDelete(UUID id) {
        serviceProviderJpaRepository.softDeleteById(id, java.time.LocalDateTime.now());
    }

    @Override
    public boolean existsByName(String name) {
        return serviceProviderJpaRepository.existsByCompanyName(name);
    }
}
