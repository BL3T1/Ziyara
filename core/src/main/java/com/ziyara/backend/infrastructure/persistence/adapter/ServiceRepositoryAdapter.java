package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ServiceMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ServiceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ServiceRepositoryAdapter
 * Implements ServiceRepository using JPA
 */
@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepository {

    private final ServiceJpaRepository jpaRepository;
    private final ServiceMapper mapper;

    @Override
    public Service save(Service service) {
        ServiceJpaEntity entity = mapper.toJpaEntity(service);
        ServiceJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Service> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void delete(Service service) {
        jpaRepository.delete(mapper.toJpaEntity(service));
    }

    @Override
    public List<Service> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByProviderId(UUID providerId) {
        return jpaRepository.findByProviderId(providerId).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByStatus(ServiceStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByType(ServiceType type) {
        return jpaRepository.findByType(type).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByProviderIdAndStatus(UUID providerId, ServiceStatus status) {
        return jpaRepository.findByProviderIdAndStatus(providerId, status).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByNameContaining(String name) {
        return jpaRepository.findByNameContaining(name).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByCity(String city) {
        return jpaRepository.findByCity(city).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByCountry(String country) {
        return jpaRepository.findByCountry(country).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByCityAndCountry(String city, String country) {
        return jpaRepository.findByCityAndCountry(city, country).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return jpaRepository.findByBasePriceBetween(minPrice, maxPrice).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByTypeAndCity(ServiceType type, String city) {
        return jpaRepository.findByTypeAndCity(type, city).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByTypeAndStatus(ServiceType type, ServiceStatus status) {
        return jpaRepository.findByTypeAndStatus(type, status).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findByLocationNear(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        return jpaRepository.findByLocationNear(latitude, longitude, radiusKm).stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Service> findAvailableServices() {
        return jpaRepository.findAvailableServices().stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public boolean hasAvailableRooms(UUID serviceId, int rooms) {
        return jpaRepository.hasAvailableRooms(serviceId, rooms);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(ServiceStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByProviderId(UUID providerId) {
        return jpaRepository.countByProviderId(providerId);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
