package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityAlertRuleJpaRepository extends JpaRepository<SecurityAlertRuleJpaEntity, UUID> {

    List<SecurityAlertRuleJpaEntity> findByEventTypeAndEnabledTrue(String eventType);

    Optional<SecurityAlertRuleJpaEntity> findFirstByName(String name);
}
