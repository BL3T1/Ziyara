package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.SecurityAlertRule;

import java.util.List;
import java.util.Optional;

public interface SecurityAlertRuleRepository {

    List<SecurityAlertRule> findByEventTypeEnabled(String eventType);

    Optional<SecurityAlertRule> findFirstByName(String name);
}
