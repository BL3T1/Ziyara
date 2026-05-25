package com.ziyara.backend.modules.payment.api;

import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.request.RefundRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.RefundResponse;
import com.ziyara.backend.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Payment module API (Phase 3 – MODULAR_MONOLITH_STRUCTURE).
 * Other modules must depend only on this interface, not on payment service implementation or repositories.
 */
public interface PaymentServiceApi {

    PaymentResponse initiatePayment(CreatePaymentRequest request);

    PaymentResponse completePayment(UUID paymentId, String transactionReference, String gateway);

    PaymentResponse completePayment(UUID paymentId, String transactionReference, String gateway,
                                   @Nullable String gatewayReference, @Nullable String threeDsStatus);

    Optional<PaymentResponse> completePaymentByGatewayReference(String gatewayReference, String gatewayName);

    Optional<PaymentResponse> failPaymentByGatewayReference(String gatewayReference, String errorMessage);

    PaymentResponse failPayment(UUID paymentId, String errorMessage);

    Page<PaymentResponse> getPayments(int page, int size, @Nullable PaymentStatus status);

    Page<PaymentResponse> pageForCustomerUserId(UUID userId, int page, int size);

    PaymentResponse getPayment(UUID id);

    PaymentResponse getByTransactionRef(String reference);

    RefundResponse refund(UUID paymentId, RefundRequest request, @Nullable UUID performedByUserId);
}
