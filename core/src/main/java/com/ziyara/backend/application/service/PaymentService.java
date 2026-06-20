package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.request.RecordCashCollectionRequest;
import com.ziyara.backend.application.dto.request.RefundRequest;
import com.ziyara.backend.application.dto.response.CashCollectionResponse;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.PaymentSummaryResponse;
import com.ziyara.backend.application.dto.response.RefundResponse;
import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.payment.GatewayChargeCommand;
import com.ziyara.backend.domain.payment.GatewayChargeResult;
import com.ziyara.backend.domain.payment.PaymentProvider;
import com.ziyara.backend.domain.usecase.payment.CompletePaymentUseCase;
import com.ziyara.backend.domain.usecase.payment.ConfirmCashBookingUseCase;
import com.ziyara.backend.domain.usecase.payment.FailPaymentUseCase;
import com.ziyara.backend.domain.usecase.payment.InitiatePaymentUseCase;
import com.ziyara.backend.domain.usecase.payment.ReconcileCashCollectionUseCase;
import com.ziyara.backend.domain.usecase.payment.RecordCashCollectionUseCase;
import com.ziyara.backend.domain.usecase.refund.RequestRefundUseCase;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.RefundRepository;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import com.ziyara.backend.infrastructure.payment.ReceiptNumberGenerator;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service: PaymentService Ã¢â‚¬â€œ implements PaymentServiceApi (Phase 3 modular monolith).
 * Handles payment processing coordination.
 */
@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentServiceApi {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CashCollectionRepository cashCollectionRepository;
    private final ReceiptNumberGenerator receiptNumberGenerator;
    private final AuditServiceApi auditLogService;
    private final PaymentGatewayProperties gatewayProperties;
    private final java.util.Optional<PaymentProvider> paymentProvider;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        log.info("Initiating payment for booking: {}", request.getBookingId());

        // Cash-first guard: when cashOnlyMode is on, force CASH and reject card requests.
        PaymentMethod method = request.getMethod();
        if (gatewayProperties.isCashOnlyMode() && isCardMethod(method)) {
            throw new BusinessException(
                    "Card payments are disabled in cash-only mode. Submit with method=CASH.");
        }

        var initResult = new InitiatePaymentUseCase(paymentRepository).execute(
                new InitiatePaymentUseCase.Input(
                        request.getBookingId(), request.getAmount(),
                        request.getCurrency() != null ? request.getCurrency() : "USD",
                        method, request.getIdempotencyKey(),
                        null, null, null));
        if (!initResult.success()) throw new BusinessException(initResult.error());

        if (initResult.wasIdempotent()) {
            log.info("Idempotent payment request, returning existing: {}", initResult.payment().getId());
            return mapToResponse(initResult.payment());
        }

        Payment saved = initResult.payment();
        // paymentToken is gateway-specific â€” not in use case, set here before gateway call
        if (request.getPaymentToken() != null && !request.getPaymentToken().isBlank()) {
            saved.setPaymentToken(request.getPaymentToken());
            saved = paymentRepository.save(saved);
        }

        if (gatewayProperties.isGatewayActive() && paymentProvider.isPresent() && isCardMethod(method)
                && request.getPaymentToken() != null && !request.getPaymentToken().isBlank()) {
            GatewayChargeCommand command = new GatewayChargeCommand(
                    saved.getId(),
                    request.getBookingId(),
                    request.getAmount(),
                    saved.getCurrency(),
                    request.getPaymentToken(),
                    request.getIdempotencyKey()
            );
            GatewayChargeResult gw = paymentProvider.get().initiatePayment(command);
            saved.setGatewayReference(gw.gatewayReference());
            saved.setTransactionReference(gw.transactionReference());
            saved.setGatewayName(gw.gatewayReference() != null ? paymentProvider.get().getProviderName() : saved.getGatewayName());
            if (gw.status() == PaymentStatus.COMPLETED) {
                var completeResult = new CompletePaymentUseCase(paymentRepository).execute(
                        new CompletePaymentUseCase.Input(saved.getId(), gw.transactionReference(),
                                gw.gatewayReference(), paymentProvider.get().getProviderName(), gw.threeDsStatus()));
                if (completeResult.success()) saved = completeResult.payment();
            } else {
                if (gw.threeDsStatus() != null) saved.setThreeDsStatus(gw.threeDsStatus());
                saved = paymentRepository.save(saved);
            }
            PaymentResponse resp = mapToResponse(saved);
            resp.setRedirectUrl(gw.redirectUrl());
            return resp;
        }

        return mapToResponse(saved);
    }

    private static boolean isCardMethod(com.ziyara.backend.domain.enums.PaymentMethod method) {
        return method == com.ziyara.backend.domain.enums.PaymentMethod.CREDIT_CARD
                || method == com.ziyara.backend.domain.enums.PaymentMethod.DEBIT_CARD
                || method == com.ziyara.backend.domain.enums.PaymentMethod.APPLE_PAY
                || method == com.ziyara.backend.domain.enums.PaymentMethod.GOOGLE_PAY;
    }
    
    @Transactional
    public PaymentResponse completePayment(UUID paymentId, String transactionReference, String gateway) {
        return completePayment(paymentId, transactionReference, gateway, null, null);
    }

    /**
     * Complete payment (e.g. after 3DS callback) with optional gateway reference and 3DS status.
     */
    @Transactional
    public PaymentResponse completePayment(UUID paymentId, String transactionReference, String gateway,
                                           @Nullable String gatewayReference, @Nullable String threeDsStatus) {
        log.info("Completing payment: {}", paymentId);
        var result = new CompletePaymentUseCase(paymentRepository).execute(
                new CompletePaymentUseCase.Input(paymentId, transactionReference, gatewayReference, gateway, threeDsStatus));
        if (!result.success()) throw new RuntimeException(result.error());
        return mapToResponse(result.payment());
    }

    /**
     * Idempotent: complete payment by gateway reference (webhook callback). If already completed, returns existing.
     */
    @Transactional
    public java.util.Optional<PaymentResponse> completePaymentByGatewayReference(String gatewayReference, String gatewayName) {
        if (gatewayReference == null || gatewayReference.isBlank()) return java.util.Optional.empty();
        return paymentRepository.findByGatewayReference(gatewayReference)
                .map(payment -> {
                    if (payment.getStatus() == PaymentStatus.COMPLETED) {
                        log.debug("Webhook idempotent: payment already completed for ref {}", gatewayReference);
                        return mapToResponse(payment);
                    }
                    var result = new CompletePaymentUseCase(paymentRepository).execute(
                            new CompletePaymentUseCase.Input(payment.getId(), gatewayReference,
                                    gatewayReference, gatewayName != null ? gatewayName : "GATEWAY", null));
                    return result.success() ? mapToResponse(result.payment()) : mapToResponse(payment);
                });
    }

    /**
     * Idempotent: fail payment by gateway reference (webhook failure callback).
     */
    @Transactional
    public java.util.Optional<PaymentResponse> failPaymentByGatewayReference(String gatewayReference, String errorMessage) {
        if (gatewayReference == null || gatewayReference.isBlank()) return java.util.Optional.empty();
        return paymentRepository.findByGatewayReference(gatewayReference)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(payment -> {
                    var result = new FailPaymentUseCase(paymentRepository).execute(
                            new FailPaymentUseCase.Input(payment.getId(), errorMessage, null));
                    Payment saved = result.success() ? result.payment() : payment;
                    staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                            .eventId(UUID.randomUUID())
                            .notificationType(NotificationType.PAYMENT_FAILED.name())
                            .title("Payment failed")
                            .message("Payment " + saved.getId() + " failed: " + (errorMessage != null ? errorMessage : ""))
                            .notifyRoles(List.of("FINANCE_MANAGER", "ACCOUNTANT", "SUPPORT_MANAGER", "SUPPORT_AGENT"))
                            .metadata("{\"paymentId\":\"" + saved.getId() + "\"}")
                            .build());
                    return mapToResponse(saved);
                });
    }
    
    @Transactional
    public PaymentResponse failPayment(UUID paymentId, String errorMessage) {
        log.warn("Failing payment: {}. Reason: {}", paymentId, errorMessage);
        var result = new FailPaymentUseCase(paymentRepository)
                .execute(new FailPaymentUseCase.Input(paymentId, errorMessage, null));
        if (!result.success()) throw new RuntimeException(result.error());
        Payment saved = result.payment();
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.PAYMENT_FAILED.name())
                .title("Payment failed")
                .message("Payment " + saved.getId() + " failed: " + (errorMessage != null ? errorMessage : ""))
                .notifyRoles(List.of("FINANCE_MANAGER", "ACCOUNTANT", "SUPPORT_MANAGER", "SUPPORT_AGENT"))
                .metadata("{\"paymentId\":\"" + saved.getId() + "\"}")
                .build());
        return mapToResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(int page, int size, @Nullable PaymentStatus status) {
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "createdAt", false);
        var result = status != null
                ? paymentRepository.findByStatus(status, query)
                : paymentRepository.findAll(query);
        return PageConverter.toSpringPage(result, query, this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> pageForCustomerUserId(UUID userId, int page, int size) {
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "createdAt", false);
        return PageConverter.toSpringPage(paymentRepository.findByCustomerUserId(userId, query), query, this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return paymentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByTransactionRef(String reference) {
        return paymentRepository.findByTransactionReference(reference)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @Transactional
    public RefundResponse refund(java.util.UUID paymentId, RefundRequest request, @Nullable UUID performedByUserId) {
        var result = new RequestRefundUseCase(paymentRepository, refundRepository).execute(
                new RequestRefundUseCase.Input(paymentId, request.getAmount(), request.getReason(), performedByUserId));
        if (!result.success()) throw new IllegalArgumentException(result.error());
        Refund saved = result.refund();

        // Mark payment as REFUNDED if fully refunded (application-level side effect)
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            java.math.BigDecimal totalRefunded = refundRepository.findByPaymentId(paymentId).stream()
                    .map(Refund::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });

        auditLogService.logAction("REFUND_CREATED", "Refund", saved.getId().toString(),
                performedByUserId, null,
                "amount=" + request.getAmount() + "|reason=" + (request.getReason() != null ? request.getReason() : ""),
                null, null);
        log.info("Refund created: {} for payment {}", saved.getId(), paymentId);
        return mapRefundToResponse(saved);
    }

    private RefundResponse mapRefundToResponse(Refund r) {
        return RefundResponse.builder()
                .id(r.getId())
                .paymentId(r.getPaymentId())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .status(r.getStatus())
                .reason(r.getReason())
                .processedBy(r.getProcessedBy())
                .transactionReference(r.getTransactionReference())
                .processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    /**
     * Confirm a cash booking — creates Payment(PENDING, CASH) without any gateway call.
     * Returns the new (or idempotent existing) payment.
     */
    @Transactional
    public PaymentResponse confirmCashBooking(UUID bookingId, BigDecimal amount, String currency, String idempotencyKey) {
        var result = new ConfirmCashBookingUseCase(paymentRepository).execute(
                new ConfirmCashBookingUseCase.Input(bookingId, amount, currency, idempotencyKey));
        if (!result.success()) throw new BusinessException(result.error());
        return mapToResponse(result.payment());
    }

    /**
     * Resolve the PENDING cash payment for a booking, then record a collection.
     * Throws {@link ResourceNotFoundException} if no PENDING cash payment exists.
     */
    @Transactional
    public CashCollectionResponse recordCashCollectionForBooking(UUID bookingId,
                                                                 UUID providerId,
                                                                 UUID collectedByUserId,
                                                                 RecordCashCollectionRequest request) {
        Payment payment = paymentRepository.findAllByBookingId(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .filter(p -> p.getMethod() == PaymentMethod.CASH
                        || p.getMethod() == PaymentMethod.CASH_ON_SERVICE
                        || p.getMethod() == PaymentMethod.CASH_ON_ARRIVAL)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No PENDING cash payment for booking " + bookingId
                        + ". Confirm the booking first."));
        return recordCashCollection(payment.getId(), providerId, collectedByUserId, request);
    }

    /**
     * Provider portal: record cash received against a payment.
     * Generates a receipt number, transitions Payment to COLLECTED, creates CashCollection.
     */
    @Transactional
    public CashCollectionResponse recordCashCollection(UUID paymentId,
                                                       UUID providerId,
                                                       UUID collectedByUserId,
                                                       RecordCashCollectionRequest request) {
        String receiptNumber = receiptNumberGenerator.next();
        var result = new RecordCashCollectionUseCase(paymentRepository, cashCollectionRepository)
                .execute(new RecordCashCollectionUseCase.Input(
                        paymentId, providerId, collectedByUserId,
                        request.getAmount(),
                        request.getCurrency(),
                        request.getCollectedAt(),
                        receiptNumber,
                        request.getNotes()));
        if (!result.success()) throw new BusinessException(result.error());

        auditLogService.logAction("CASH_COLLECTED", "CashCollection",
                result.collection().getId().toString(),
                collectedByUserId, null,
                "paymentId=" + paymentId + "|amount=" + request.getAmount() + "|receipt=" + receiptNumber,
                null, null);

        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.PAYMENT_SUCCESS.name())
                .title("Cash collected")
                .message("Cash collection " + receiptNumber + " recorded for payment " + paymentId)
                .notifyRoles(List.of("FINANCE_MANAGER", "ACCOUNTANT"))
                .metadata("{\"paymentId\":\"" + paymentId + "\",\"receipt\":\"" + receiptNumber + "\"}")
                .build());

        return mapCollectionToResponse(result.collection());
    }

    /**
     * Admin/finance: reconcile a cash collection. Idempotent.
     */
    @Transactional
    public CashCollectionResponse reconcileCashCollection(UUID collectionId, UUID adminUserId, String notes) {
        var result = new ReconcileCashCollectionUseCase(cashCollectionRepository, paymentRepository)
                .execute(new ReconcileCashCollectionUseCase.Input(collectionId, adminUserId, notes));
        if (!result.success()) throw new BusinessException(result.error());

        if (!result.wasIdempotent()) {
            auditLogService.logAction("CASH_RECONCILED", "CashCollection",
                    collectionId.toString(), adminUserId, null,
                    "notes=" + (notes != null ? notes : ""), null, null);
        }
        return mapCollectionToResponse(result.collection());
    }

    @Transactional(readOnly = true)
    public BigDecimal sumOpenCashForProvider(UUID providerId) {
        return cashCollectionRepository.sumOpenForProvider(providerId);
    }

    @Transactional(readOnly = true)
    public List<CashCollectionResponse> listOpenCashForProvider(UUID providerId) {
        return cashCollectionRepository.findOpenForProvider(providerId).stream()
                .map(this::mapCollectionToResponse)
                .toList();
    }

    private CashCollectionResponse mapCollectionToResponse(CashCollection c) {
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

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .gatewayReference(payment.getGatewayReference())
                .threeDsStatus(payment.getThreeDsStatus())
                .gatewayName(payment.getGatewayName())
                .errorMessage(payment.getErrorMessage())
                .processedAt(payment.getProcessedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary() {
        BigDecimal collected = paymentRepository.sumByStatus(PaymentStatus.COMPLETED);
        BigDecimal pending   = paymentRepository.sumByStatus(PaymentStatus.PENDING);
        BigDecimal refunded  = paymentRepository.sumByStatus(PaymentStatus.REFUNDED);
        return PaymentSummaryResponse.builder()
                .totalCollected(collected != null ? collected : BigDecimal.ZERO)
                .totalPending(pending   != null ? pending   : BigDecimal.ZERO)
                .totalRefunded(refunded != null ? refunded  : BigDecimal.ZERO)
                .currency("USD")
                .build();
    }
}

