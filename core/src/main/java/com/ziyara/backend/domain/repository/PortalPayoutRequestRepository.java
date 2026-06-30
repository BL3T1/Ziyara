package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.PortalPayoutRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortalPayoutRequestRepository {

    PortalPayoutRequest save(PortalPayoutRequest request);

    Optional<PortalPayoutRequest> findById(UUID id);

    List<PortalPayoutRequest> findFiltered(String status, UUID providerId,
                                            String start, String end, String q,
                                            int limit, long offset);

    long countFiltered(String status, UUID providerId, String start, String end, String q);

    List<PortalPayoutRequest> findByProviderId(UUID providerId, int limit, long offset);

    long countByProviderId(UUID providerId);

    List<PortalPayoutRequest> findForExport(String status, String start, String end);

    BigDecimal sumAmountByStatus(String status);

    long countByStatus(String status);

    BigDecimal sumCompletedAmountInPeriod(String start, String end);

    long countByStatuses(List<String> statuses);

    BigDecimal sumPendingAmountByProvider(UUID providerId);
}
