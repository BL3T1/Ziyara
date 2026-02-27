package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreatePaymentRequest;
import com.ziyarah.application.dto.response.PaymentResponse;
import com.ziyarah.domain.entity.Payment;
import com.ziyarah.domain.enums.PaymentStatus;
import com.ziyarah.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: PaymentService
 * Handles payment processing coordination
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        log.info("Initiating payment for booking: {}", request.getBookingId());
        
        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentToken(request.getPaymentToken());
        
        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }
    
    @Transactional
    public PaymentResponse completePayment(UUID paymentId, String transactionReference, String gateway) {
        log.info("Completing payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(transactionReference);
        payment.setGatewayName(gateway);
        payment.setProcessedAt(java.time.LocalDateTime.now());
        
        return mapToResponse(paymentRepository.save(payment));
    }
    
    @Transactional
    public PaymentResponse failPayment(UUID paymentId, String errorMessage) {
        log.warn("Failing payment: {}. Reason: {}", paymentId, errorMessage);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        payment.setProcessedAt(java.time.LocalDateTime.now());
        
        return mapToResponse(paymentRepository.save(payment));
    }
    
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                .gatewayName(payment.getGatewayName())
                .errorMessage(payment.getErrorMessage())
                .processedAt(payment.getProcessedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
