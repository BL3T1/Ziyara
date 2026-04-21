package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.request.RefundRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.RefundResponse;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.enums.RefundStatus;
import com.ziyara.backend.application.dto.payment.GatewayPaymentResponse;
import com.ziyara.backend.application.dto.payment.TokenizedPaymentCommand;
import com.ziyara.backend.domain.payment.PaymentProvider;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.RefundRepository;
import com.ziyara.backend.infrastructure.config.PaymentGatewayProperties;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service: PaymentService – implements PaymentServiceApi (Phase 3 modular monolith).
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
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            java.util.Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent payment request, returning existing: {}", existing.get().getId());
                return mapToResponse(existing.get());
            }
        }
        log.info("Initiating payment for booking: {}", request.getBookingId());

        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentToken(request.getPaymentToken());
        payment.setIdempotencyKey(request.getIdempotencyKey());

        Payment saved = paymentRepository.save(payment);

        if (gatewayProperties.isEnabled() && paymentProvider.isPresent() && isCardMethod(request.getMethod())
                && request.getPaymentToken() != null && !request.getPaymentToken().isBlank()) {
            TokenizedPaymentCommand command = TokenizedPaymentCommand.builder()
                    .paymentId(saved.getId())
                    .bookingId(request.getBookingId())
                    .amount(request.getAmount())
                    .currency(saved.getCurrency())
                    .paymentToken(request.getPaymentToken())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();
            GatewayPaymentResponse gw = paymentProvider.get().initiatePayment(command);
            saved.setGatewayReference(gw.getGatewayReference());
            saved.setTransactionReference(gw.getTransactionReference());
            saved.setGatewayName(gw.getGatewayReference() != null ? paymentProvider.get().getProviderName() : saved.getGatewayName());
            if (gw.getThreeDsStatus() != null) saved.setThreeDsStatus(gw.getThreeDsStatus());
            if (gw.getStatus() == PaymentStatus.COMPLETED) {
                saved.setStatus(PaymentStatus.COMPLETED);
                saved.setProcessedAt(java.time.LocalDateTime.now());
            }
            saved = paymentRepository.save(saved);
            PaymentResponse resp = mapToResponse(saved);
            resp.setRedirectUrl(gw.getRedirectUrl());
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
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(transactionReference);
        payment.setGatewayName(gateway);
        if (gatewayReference != null) payment.setGatewayReference(gatewayReference);
        if (threeDsStatus != null) payment.setThreeDsStatus(threeDsStatus);
        payment.setProcessedAt(java.time.LocalDateTime.now());
        return mapToResponse(paymentRepository.save(payment));
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
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setTransactionReference(gatewayReference);
                    payment.setGatewayReference(gatewayReference);
                    payment.setGatewayName(gatewayName != null ? gatewayName : "GATEWAY");
                    payment.setProcessedAt(java.time.LocalDateTime.now());
                    return mapToResponse(paymentRepository.save(payment));
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
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorMessage(errorMessage);
                    payment.setProcessedAt(java.time.LocalDateTime.now());
                    Payment saved = paymentRepository.save(payment);
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
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        payment.setProcessedAt(java.time.LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
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
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> pg = status != null
                ? paymentRepository.findByStatus(status, pr)
                : paymentRepository.findAll(pr);
        return pg.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> pageForCustomerUserId(UUID userId, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentRepository.findByCustomerUserId(userId, pr).map(this::mapToResponse);
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
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Payment must be COMPLETED to refund");
        }
        java.math.BigDecimal alreadyRefunded = refundRepository.findByPaymentId(paymentId).stream()
                .filter(r -> r.getStatus() == RefundStatus.PROCESSED || r.getStatus() == RefundStatus.REQUESTED || r.getStatus() == RefundStatus.PENDING || r.getStatus() == RefundStatus.APPROVED)
                .map(Refund::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal refundable = payment.getAmount().subtract(alreadyRefunded);
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds refundable amount: " + refundable);
        }
        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(request.getAmount());
        refund.setCurrency(payment.getCurrency());
        refund.setStatus(RefundStatus.REQUESTED);
        refund.setReason(request.getReason());
        refund.setProcessedBy(performedByUserId);
        Refund saved = refundRepository.save(refund);
        if (refundable.subtract(request.getAmount()).compareTo(java.math.BigDecimal.ZERO) <= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }
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
