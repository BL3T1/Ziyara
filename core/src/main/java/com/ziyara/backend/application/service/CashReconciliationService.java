package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.CashCollectionResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.enums.CashCollectionStatus;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.usecase.payment.ForfeitNoShowDepositUseCase;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin/finance-side service for cash reconciliation reports and the no-show
 * forfeit flow. The provider-side record/reconcile actions live on
 * {@link PaymentService} (per CASH_FIRST_PAYMENT_PLAN §6.1).
 */
@Service
@RequiredArgsConstructor
public class CashReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(CashReconciliationService.class);

    private final CashCollectionRepository cashCollectionRepository;
    private final PaymentRepository paymentRepository;
    private final AuditServiceApi auditLogService;

    /**
     * Daily cash sheet for a provider — what was collected on that calendar day.
     */
    @Transactional(readOnly = true)
    public List<CashCollectionResponse> dailyCashSheet(UUID providerId, LocalDate day) {
        return cashCollectionRepository.findByProviderIdAndDay(providerId, day).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Admin reconciliation queue: every collection in OPEN status, paged.
     */
    @Transactional(readOnly = true)
    public Page<CashCollectionResponse> pendingReconciliation(int page, int size) {
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "collectedAt", false);
        PagedResult<CashCollection> result = cashCollectionRepository.findByStatus(CashCollectionStatus.OPEN, query);
        return PageConverter.toSpringPage(result, query, this::toResponse);
    }

    /**
     * Admin marks a collection as DISPUTED. Payment remains in COLLECTED.
     */
    @Transactional
    public CashCollectionResponse dispute(UUID collectionId, UUID adminUserId, String reason) {
        CashCollection collection = cashCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new BusinessException("Cash collection not found"));
        if (collection.getStatus() == CashCollectionStatus.RECONCILED) {
            throw new BusinessException("Already-reconciled collections cannot be disputed");
        }
        collection.dispute(adminUserId, reason);
        CashCollection saved = cashCollectionRepository.save(collection);
        auditLogService.logAction("CASH_DISPUTED", "CashCollection",
                collectionId.toString(), adminUserId, null,
                "reason=" + (reason != null ? reason : ""), null, null);
        return toResponse(saved);
    }

    /**
     * Booking no-show: forfeit any COMPLETED deposits, cancel PENDING cash.
     */
    @Transactional
    public int forfeitNoShow(UUID bookingId, UUID adminUserId) {
        var result = new ForfeitNoShowDepositUseCase(paymentRepository)
                .execute(new ForfeitNoShowDepositUseCase.Input(bookingId, adminUserId));
        if (!result.success()) throw new BusinessException(result.error());
        int n = result.updatedPayments().size();
        if (n > 0) {
            auditLogService.logAction("BOOKING_NO_SHOW_FORFEIT", "Booking",
                    bookingId.toString(), adminUserId, null,
                    "paymentsUpdated=" + n, null, null);
        }
        return n;
    }

    private CashCollectionResponse toResponse(CashCollection c) {
        return CashCollectionResponse.builder()
                .id(c.getId())
                .paymentId(c.getPaymentId())
                .providerId(c.getProviderId())
                .collectedAt(c.getCollectedAt())
                .collectedByUserId(c.getCollectedByUserId())
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .receiptNumber(c.getReceiptNumber())
                .notes(c.getNotes())
                .reconciledAt(c.getReconciledAt())
                .reconciledByUserId(c.getReconciledByUserId())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
