package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.RestMenuSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestMenuSectionRepository {

    RestMenuSection save(RestMenuSection section);

    Optional<RestMenuSection> findById(UUID id);

    List<RestMenuSection> findByServiceId(UUID serviceId);

    long countByServiceId(UUID serviceId);

    void deleteById(UUID id);
}
