package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ServiceImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ServiceImageRepository
 */
public interface ServiceImageRepository {
    ServiceImage save(ServiceImage image);

    Optional<ServiceImage> findById(UUID id);

    List<ServiceImage> findByServiceIdOrdered(UUID serviceId);

    List<ServiceImage> findByServiceId(UUID serviceId);

    Optional<ServiceImage> findByServiceIdAndIsPrimary(UUID serviceId, boolean isPrimary);

    long countByServiceId(UUID serviceId);

    void deleteById(UUID id);

    int clearPrimaryByServiceId(UUID serviceId);

    int clearPrimaryByServiceIdExcept(UUID serviceId, UUID excludeId);
}
