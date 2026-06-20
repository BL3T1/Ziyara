package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.DataRetentionPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataRetentionPolicyJpaRepository extends JpaRepository<DataRetentionPolicyJpaEntity, UUID> {

    List<DataRetentionPolicyJpaEntity> findByEnabledTrue();
}
