package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ServiceImage;
import com.ziyara.backend.domain.repository.ServiceImageRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceImageJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ServiceImageMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ServiceImageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ServiceImageRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class ServiceImageRepositoryAdapter implements ServiceImageRepository {
    
    private final ServiceImageJpaRepository serviceImageJpaRepository;
    private final ServiceImageMapper serviceImageMapper;
    
    @Override
    public ServiceImage save(ServiceImage image) {
        ServiceImageJpaEntity entity = serviceImageMapper.toJpaEntity(image);
        ServiceImageJpaEntity savedEntity = serviceImageJpaRepository.save(entity);
        return serviceImageMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<ServiceImage> findById(UUID id) {
        return serviceImageJpaRepository.findById(id)
                .map(serviceImageMapper::toDomainEntity);
    }
    
    @Override
    public List<ServiceImage> findByServiceIdOrdered(UUID serviceId) {
        return serviceImageJpaRepository.findByServiceIdOrderByDisplayOrderAscIdAsc(serviceId).stream()
                .map(serviceImageMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceImage> findByServiceId(UUID serviceId) {
        return serviceImageJpaRepository.findByServiceId(serviceId).stream()
                .map(serviceImageMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByServiceId(UUID serviceId) {
        return serviceImageJpaRepository.countByServiceId(serviceId);
    }
    
    @Override
    public Optional<ServiceImage> findByServiceIdAndIsPrimary(UUID serviceId, boolean isPrimary) {
        return serviceImageJpaRepository.findByServiceIdAndIsPrimary(serviceId, isPrimary)
                .map(serviceImageMapper::toDomainEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        serviceImageJpaRepository.deleteById(id);
    }

    @Override
    public int clearPrimaryByServiceId(UUID serviceId) {
        return serviceImageJpaRepository.clearPrimaryByServiceId(serviceId);
    }

    @Override
    public int clearPrimaryByServiceIdExcept(UUID serviceId, UUID excludeId) {
        return serviceImageJpaRepository.clearPrimaryByServiceIdExcept(serviceId, excludeId);
    }
}
