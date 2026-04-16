package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Page<ServiceProvider> findAll(Pageable pageable);

    Page<ServiceProvider> findByStatus(ProviderStatus status, Pageable pageable);

    long count();
    void deleteById(UUID id);
    boolean existsByName(String name);
}
