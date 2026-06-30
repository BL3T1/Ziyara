package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AdminPayoutActionRequest;
import com.ziyara.backend.application.dto.request.BulkPayoutActionRequest;
import com.ziyara.backend.application.dto.request.CreateManualPayoutRequest;
import com.ziyara.backend.application.dto.response.AdminPayoutResponse;
import com.ziyara.backend.application.dto.response.AdminPayoutSummaryResponse;
import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.PortalPayoutRequest;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.PortalPayoutRequestRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPayoutService {

    private static final String CURRENCY = "USD";

    private final PortalPayoutRequestRepository payoutRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AdminPayoutSummaryResponse getSummary(String start, String end) {
        BigDecimal totalPayable = payoutRepository.sumAmountByStatus("PENDING");
        long pendingCount = payoutRepository.countByStatus("PENDING");
        long processingCount = payoutRepository.countByStatus("PROCESSING");
        BigDecimal totalCompleted = payoutRepository.sumCompletedAmountInPeriod(start, end);
        long failedOnHold = payoutRepository.countByStatuses(List.of("FAILED", "ON_HOLD"));

        return AdminPayoutSummaryResponse.builder()
                .totalPayable(totalPayable)
                .pendingCount(pendingCount)
                .processingCount(processingCount)
                .totalCompletedInPeriod(totalCompleted)
                .failedOnHoldCount(failedOnHold)
                .currency(CURRENCY)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminPayoutResponse> listPayouts(int page, int size, String status, String providerId,
                                                  String start, String end, String q) {
        UUID providerIdUUID = providerId != null && !providerId.isBlank() ? UUID.fromString(providerId) : null;
        long total = payoutRepository.countFiltered(status, providerIdUUID, start, end, q);
        List<PortalPayoutRequest> requests = payoutRepository.findFiltered(
                status, providerIdUUID, start, end, q, size, (long) page * size);

        Map<UUID, ServiceProvider> providerMap = loadProviders(requests);
        List<AdminPayoutResponse> content = requests.stream()
                .map(r -> toResponse(r, providerMap.get(r.getProviderId())))
                .collect(Collectors.toList());

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    @Transactional(readOnly = true)
    public AdminPayoutResponse getById(UUID id) {
        PortalPayoutRequest req = findOrThrow(id);
        ServiceProvider provider = serviceProviderRepository.findById(req.getProviderId()).orElse(null);
        return toResponse(req, provider);
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_APPROVE", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse approve(UUID id, AdminPayoutActionRequest req, UUID actorId) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.approve(actorId, req != null ? req.getNotes() : null);
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_HOLD", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse hold(UUID id) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.hold();
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_RELEASE_HOLD", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse releaseHold(UUID id) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.releaseHold();
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_CANCEL", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse cancel(UUID id) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.cancel();
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_RETRY", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse retry(UUID id) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.retry();
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_MARK_PAID", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse markPaid(UUID id, AdminPayoutActionRequest req, UUID actorId) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.markPaid(actorId,
                req != null ? req.getTransactionId() : null,
                req != null ? req.getNotes() : null);
        payoutRepository.save(payout);
        return getById(id);
    }

    @Audited(action = "PAYOUT_SCHEDULE", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse schedule(UUID id, AdminPayoutActionRequest req) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.schedule(req != null ? req.getScheduledAt() : null);
        payoutRepository.save(payout);
        return getById(id);
    }

    @Transactional
    public AdminPayoutResponse updateNotes(UUID id, String notes) {
        PortalPayoutRequest payout = findOrThrow(id);
        payout.setNotes(notes);
        payoutRepository.save(payout);
        return getById(id);
    }

    // -------------------------------------------------------------------------
    // Bulk operations
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_BULK_APPROVE", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkApprove(BulkPayoutActionRequest req, UUID actorId) {
        int count = 0;
        List<String> failed = new ArrayList<>();
        for (UUID id : req.getIds()) {
            try {
                PortalPayoutRequest payout = findOrThrow(id);
                payout.approve(actorId, null);
                payoutRepository.save(payout);
                count++;
            } catch (Exception e) {
                failed.add(id.toString());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", count);
        result.put("failed", failed);
        return result;
    }

    @Audited(action = "PAYOUT_BULK_HOLD", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkHold(BulkPayoutActionRequest req) {
        int count = 0;
        for (UUID id : req.getIds()) {
            try {
                PortalPayoutRequest payout = findOrThrow(id);
                payout.hold();
                payoutRepository.save(payout);
                count++;
            } catch (Exception e) {
                // skip if status is not PENDING
            }
        }
        return Map.of("processed", count);
    }

    @Audited(action = "PAYOUT_BULK_RELEASE_HOLD", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkReleaseHold(BulkPayoutActionRequest req) {
        int count = 0;
        for (UUID id : req.getIds()) {
            try {
                PortalPayoutRequest payout = findOrThrow(id);
                payout.releaseHold();
                payoutRepository.save(payout);
                count++;
            } catch (Exception e) {
                // skip if status is not ON_HOLD
            }
        }
        return Map.of("processed", count);
    }

    // -------------------------------------------------------------------------
    // Manual payout creation
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_CREATE_MANUAL", entityType = "Payout")
    @Transactional
    public AdminPayoutResponse createManual(CreateManualPayoutRequest req, UUID actorId) {
        PortalPayoutRequest payout = new PortalPayoutRequest();
        payout.setId(UUID.randomUUID());
        payout.setProviderId(req.getProviderId());
        payout.setAmount(req.getAmount());
        payout.setCurrency(CURRENCY);
        payout.setNotes(req.getMemo());
        payout.setStatus(req.isExecuteImmediately() ? "PROCESSING" : "PENDING");
        payout.setRequestedAt(Instant.now());
        payout.setManual(true);
        payout.setProcessedBy(actorId);
        payoutRepository.save(payout);
        return getById(payout.getId());
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] exportCsv(String status, String start, String end) {
        List<PortalPayoutRequest> rows = payoutRepository.findForExport(status, start, end);
        Set<UUID> providerIds = rows.stream()
                .map(PortalPayoutRequest::getProviderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, ServiceProvider> providerMap = serviceProviderRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(ServiceProvider::getId, Function.identity(), (a, b) -> a));

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Provider,Email,Amount,Currency,Status,Requested At,Processed At,Transaction ID,Notes,Manual\n");
        for (PortalPayoutRequest r : rows) {
            ServiceProvider sp = providerMap.get(r.getProviderId());
            csv.append('"').append(r.getId()).append('"').append(',');
            csv.append('"').append(safe(sp != null ? sp.getCompanyName() : null)).append('"').append(',');
            csv.append('"').append(safe(sp != null ? sp.getContactEmail() : null)).append('"').append(',');
            csv.append(r.getAmount()).append(',');
            csv.append(safe(r.getCurrency())).append(',');
            csv.append(safe(r.getStatus())).append(',');
            csv.append(r.getRequestedAt() != null ? r.getRequestedAt().toString() : "").append(',');
            csv.append(r.getProcessedAt() != null ? r.getProcessedAt().toString() : "").append(',');
            csv.append('"').append(safe(r.getTransactionId())).append('"').append(',');
            csv.append('"').append(safe(r.getNotes())).append('"').append(',');
            csv.append(r.isManual()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PortalPayoutRequest findOrThrow(UUID id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout request not found: " + id));
    }

    private Map<UUID, ServiceProvider> loadProviders(List<PortalPayoutRequest> requests) {
        Set<UUID> ids = requests.stream()
                .map(PortalPayoutRequest::getProviderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return serviceProviderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ServiceProvider::getId, Function.identity(), (a, b) -> a));
    }

    private AdminPayoutResponse toResponse(PortalPayoutRequest r, ServiceProvider sp) {
        return AdminPayoutResponse.builder()
                .id(r.getId())
                .providerId(r.getProviderId())
                .providerName(sp != null ? sp.getCompanyName() : null)
                .providerEmail(sp != null ? sp.getContactEmail() : null)
                .amount(r.getAmount())
                .currency(r.getCurrency() != null ? r.getCurrency() : CURRENCY)
                .notes(r.getNotes())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .processedBy(r.getProcessedBy() != null ? r.getProcessedBy().toString() : null)
                .rejectionReason(r.getRejectionReason())
                .transactionId(r.getTransactionId())
                .scheduledAt(r.getScheduledAt() != null ? Instant.parse(r.getScheduledAt()) : null)
                .manual(r.isManual())
                .statusHistory(List.of())
                .build();
    }
}
