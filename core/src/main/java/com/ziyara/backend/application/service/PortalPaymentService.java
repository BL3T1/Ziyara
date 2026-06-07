package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.ApproveCashPaymentRequest;
import com.ziyara.backend.application.dto.request.RecordPaymentRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PortalPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> listBookingPayments(UUID bookingId, UUID providerId) {
        verifyBookingOwnership(bookingId, providerId);
        return paymentRepository.findAllByBookingId(bookingId)
                .stream().map(this::toResponse).toList();
    }

    public PaymentResponse approveCashPayment(UUID bookingId, UUID providerId, ApproveCashPaymentRequest request) {
        Booking booking = verifyBookingOwnership(bookingId, providerId);

        PaymentMethod method = booking.getPaymentMethod();
        if (method == null || !method.name().startsWith("CASH")) {
            throw new BusinessException("Booking payment method is not cash");
        }
        if (!booking.isPaymentPending()) {
            throw new BusinessException("Booking is already marked as paid");
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.COLLECTED);
        payment.setCategory("PORTAL_CASH_APPROVAL");
        payment.setTransactionReference(request.getNotes());
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        booking.markPaid();
        bookingRepository.save(booking);

        return toResponse(saved);
    }

    public PaymentResponse recordPayment(UUID bookingId, UUID providerId, RecordPaymentRequest request) {
        Booking booking = verifyBookingOwnership(bookingId, providerId);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.RECORDED);
        payment.setCategory("PORTAL_MANUAL_ENTRY");
        payment.setTransactionReference(request.getTransactionReference());
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        BigDecimal recorded = paymentRepository.findAllByBookingId(bookingId).stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (booking.getTotalAmount() != null
                && recorded.compareTo(booking.getTotalAmount()) >= 0) {
            booking.markPaid();
            bookingRepository.save(booking);
        }

        return toResponse(saved);
    }

    private Booking verifyBookingOwnership(UUID bookingId, UUID providerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(booking.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(svc.getProviderId())) {
            throw new AccessDeniedException("Booking does not belong to this provider");
        }
        return booking;
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .method(p.getMethod())
                .status(p.getStatus())
                .transactionReference(p.getTransactionReference())
                .processedAt(p.getProcessedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
