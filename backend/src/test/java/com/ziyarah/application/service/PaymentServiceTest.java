package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreatePaymentRequest;
import com.ziyarah.application.dto.response.PaymentResponse;
import com.ziyarah.domain.entity.Payment;
import com.ziyarah.domain.enums.PaymentStatus;
import com.ziyarah.domain.enums.PaymentMethod;
import com.ziyarah.domain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initiatePayment_ShouldReturnResponse() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .method(PaymentMethod.CREDIT_CARD)
                .build();

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.initiatePayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.getStatus());
    }

    @Test
    void completePayment_ShouldUpdateStatus() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.completePayment(paymentId, "REF123", "STRIPE");

        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals("REF123", response.getTransactionReference());
    }
}
