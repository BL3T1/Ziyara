package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {
    Optional<CustomerJpaEntity> findByUserId(UUID userId);
    List<CustomerJpaEntity> findByIdDocumentUrlNotNullAndIdentityVerified(Boolean verified);
    List<CustomerJpaEntity> findByIdentityVerifiedTrue();
}
