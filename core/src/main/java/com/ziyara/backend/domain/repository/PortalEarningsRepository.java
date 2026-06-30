package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ServiceEarningData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PortalEarningsRepository {
    List<ServiceEarningData> findServiceEarnings(UUID providerId, LocalDate start, LocalDate end);
}
