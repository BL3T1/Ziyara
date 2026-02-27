package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.ServiceProvider;
import com.ziyarah.domain.enums.ProviderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ServiceProviderRepository
 */
public interface ServiceProviderRepository {
    ServiceProvider save(ServiceProvider provider);
    Optional<ServiceProvider> findById(UUID id);
    Optional<ServiceProvider> findByUserId(UUID userId);
    Optional<ServiceProvider> findByName(String name);
    List<ServiceProvider> findByStatus(ProviderStatus status);
    List<ServiceProvider> findByType(String type);
    List<ServiceProvider> findAll();
    void deleteById(UUID id);
    boolean existsByName(String name);
}
