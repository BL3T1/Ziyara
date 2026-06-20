package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.PiiFieldRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PiiFieldRegistryRepository {

    PiiFieldRegistry save(PiiFieldRegistry entry);

    Optional<PiiFieldRegistry> findById(UUID id);

    List<PiiFieldRegistry> findAll();

    void deleteById(UUID id);
}
