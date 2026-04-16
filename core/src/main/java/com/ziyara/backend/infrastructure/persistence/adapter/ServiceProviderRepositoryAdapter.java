package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ServiceProviderMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ServiceProviderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
        return serviceProviderJpaRepository.findByType(type).stream()
                .map(serviceProviderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ServiceProvider> findAll() {
        return serviceProviderJpaRepository.findAll().stream()
                .map(serviceProviderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ServiceProvider> findAll(Pageable pageable) {
        return serviceProviderJpaRepository.findAll(pageable).map(serviceProviderMapper::toDomainEntity);
    }

    @Override
    public Page<ServiceProvider> findByStatus(ProviderStatus status, Pageable pageable) {
        return serviceProviderJpaRepository.findByStatus(status, pageable).map(serviceProviderMapper::toDomainEntity);
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
    public boolean existsByName(String name) {
        return serviceProviderJpaRepository.existsByCompanyName(name);
    }
}
