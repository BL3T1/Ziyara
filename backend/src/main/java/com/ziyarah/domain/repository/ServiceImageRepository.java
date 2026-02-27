package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.ServiceImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ServiceImageRepository
 */
public interface ServiceImageRepository {
    ServiceImage save(ServiceImage image);
    Optional<ServiceImage> findById(UUID id);
    List<ServiceImage> findByServiceId(UUID serviceId);
    Optional<ServiceImage> findByServiceIdAndIsPrimary(UUID serviceId, boolean isPrimary);
    void deleteById(UUID id);
}
