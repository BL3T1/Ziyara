package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.RefundRepository;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import com.ziyara.backend.infrastructure.payment.ReceiptNumberGenerator;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private CashCollectionRepository cashCollectionRepository;

    @Mock
    private ReceiptNumberGenerator receiptNumberGenerator;

    @Mock
    private com.ziyara.backend.modules.sys.api.AuditServiceApi auditLogService;

    @Mock
    private PaymentGatewayProperties gatewayProperties;

    @Mock
    private StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(gatewayProperties.isEnabled()).thenReturn(false);
        lenient().when(gatewayProperties.isCashOnlyMode()).thenReturn(false);
        lenient().when(gatewayProperties.isGatewayActive()).thenReturn(false);
        paymentService = new PaymentService(
                paymentRepository,
                refundRepository,
                cashCollectionRepository,
                receiptNumberGenerator,
                auditLogService,
                gatewayProperties,
                Optional.empty(),
                staffNotificationCommandPublisher);
    }

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

