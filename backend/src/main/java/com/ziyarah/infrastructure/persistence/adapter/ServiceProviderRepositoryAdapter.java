package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.ServiceProvider;
import com.ziyarah.domain.enums.ProviderStatus;
import com.ziyarah.domain.repository.ServiceProviderRepository;
import com.ziyarah.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.ServiceProviderMapper;
import com.ziyarah.infrastructure.persistence.repository.ServiceProviderJpaRepository;
import lombok.RequiredArgsConstructor;
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
        return serviceProviderJpaRepository.findByUserId(userId)
                .map(serviceProviderMapper::toDomainEntity);
    }
    
    @Override
    public Optional<ServiceProvider> findByName(String name) {
        return serviceProviderJpaRepository.findByName(name)
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
    public void deleteById(UUID id) {
        serviceProviderJpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByName(String name) {
        return serviceProviderJpaRepository.existsByName(name);
    }
}
