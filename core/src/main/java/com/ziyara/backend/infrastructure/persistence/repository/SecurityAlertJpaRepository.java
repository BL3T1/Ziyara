package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityAlertJpaRepository extends JpaRepository<SecurityAlertJpaEntity, UUID> {
}
