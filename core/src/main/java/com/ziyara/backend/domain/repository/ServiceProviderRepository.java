package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;

import java.time.LocalDate;
import java.util.Collection;
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

    PagedResult<ServiceProvider> findAll(PageQuery pageQuery);

    PagedResult<ServiceProvider> findByStatus(ProviderStatus status, PageQuery pageQuery);

    PagedResult<ServiceProvider> findByProviderType(String providerType, PageQuery pageQuery);

    PagedResult<ServiceProvider> findByStatusAndProviderType(ProviderStatus status, String providerType, PageQuery pageQuery);

    List<ServiceProvider> findByExpiryDate(LocalDate date);

    List<ServiceProvider> findAllById(Collection<UUID> ids);

    long count();
    void deleteById(UUID id);
    void softDelete(UUID id);
    boolean existsByName(String name);
}
