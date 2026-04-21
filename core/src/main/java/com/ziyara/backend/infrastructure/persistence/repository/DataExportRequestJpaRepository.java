package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.DataExportRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataExportRequestJpaRepository extends JpaRepository<DataExportRequestJpaEntity, UUID> {

    List<DataExportRequestJpaEntity> findByUserIdOrderByRequestedAtDesc(UUID userId);
}
