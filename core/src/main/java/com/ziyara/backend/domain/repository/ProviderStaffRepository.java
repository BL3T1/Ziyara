package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ProviderStaff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderStaffRepository {

    ProviderStaff save(ProviderStaff staff);

    Optional<ProviderStaff> findById(UUID id);

    List<ProviderStaff> findByProviderId(UUID providerId);

    /** Returns the number of staff members (excluding owner) linked to the provider. */
    long countByProviderId(UUID providerId);

    Optional<ProviderStaff> findByProviderIdAndUserId(UUID providerId, UUID userId);

    Optional<ProviderStaff> findByUserId(UUID userId);

    void deleteById(UUID id);
}
