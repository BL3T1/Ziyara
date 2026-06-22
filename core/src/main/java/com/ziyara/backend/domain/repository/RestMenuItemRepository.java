package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.RestMenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestMenuItemRepository {

    RestMenuItem save(RestMenuItem item);

    Optional<RestMenuItem> findById(UUID id);

    List<RestMenuItem> findBySectionId(UUID sectionId);

    List<RestMenuItem> findAllById(List<UUID> ids);

    long countBySectionId(UUID sectionId);

    void deleteById(UUID id);
}
