package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.FeatureFlag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository {

    FeatureFlag save(FeatureFlag featureFlag);

    Optional<FeatureFlag> findById(UUID id);

    Optional<FeatureFlag> findByKey(String flagKey);

    List<FeatureFlag> findAll();

    void deleteById(UUID id);
}
