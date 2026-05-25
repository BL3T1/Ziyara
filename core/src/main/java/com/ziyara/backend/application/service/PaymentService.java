package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.request.RefundRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.RefundResponse;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.payment.GatewayChargeCommand;
import com.ziyara.backend.domain.payment.GatewayChargeResult;
import com.ziyara.backend.domain.payment.PaymentProvider;
import com.ziyara.backend.domain.usecase.payment.CompletePaymentUseCase;
import com.ziyara.backend.domain.usecase.payment.FailPaymentUseCase;
import com.ziyara.backend.domain.usecase.payment.InitiatePaymentUseCase;
import com.ziyara.backend.domain.usecase.refund.RequestRefundUseCase;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.RefundRepository;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuditServiceApi auditLogService;
    private final PaymentGatewayProperties gatewayProperties;
    private final java.util.Optional<PaymentProvider> paymentProvider;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        log.info("Initiating payment for booking: {}", request.getBookingId());

        var initResult = new InitiatePaymentUseCase(paymentRepository).execute(
                new InitiatePaymentUseCase.Input(
                        request.getBookingId(), request.getAmount(),
                        request.getCurrency() != null ? request.getCurrency() : "USD",
                        request.getMethod(), request.getIdempotencyKey(),
                        null, null, null));
        if (!initResult.success()) throw new com.ziyara.backend.application.exception.BusinessException(initResult.error());

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

        if (gatewayProperties.isEnabled() && paymentProvider.isPresent() && isCardMethod(request.getMethod())
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
                            .notifyRoles(List.of("FINANCE_MANAGER", "ACCOUNTANT", "SUPPORT_MANAGER"))
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
                .notifyRoles(List.of("FINANCE_MANAGER", "ACCOUNTANT", "SUPPORT_MANAGER"))
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
}

