package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.UserConsentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserConsentJpaRepository extends JpaRepository<UserConsentJpaEntity, UUID> {

    List<UserConsentJpaEntity> findByUserIdOrderByGrantedAtDesc(UUID userId);
}
