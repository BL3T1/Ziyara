package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.ApproveCashPaymentRequest;
import com.ziyara.backend.application.dto.request.RecordPaymentRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.BookingPaymentStatus;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalPaymentServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ServiceRepository serviceRepository;

    @InjectMocks PortalPaymentService service;

    private static final UUID PROVIDER_ID = UUID.randomUUID();
    private static final UUID OTHER_PROVIDER = UUID.randomUUID();
    private static final UUID BOOKING_ID    = UUID.randomUUID();
    private static final UUID SERVICE_ID    = UUID.randomUUID();

    // ── listBookingPayments ───────────────────────────────────────────────────

    @Test
    void listPayments_bookingNotFound_throwsNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBookingPayments(BOOKING_ID, PROVIDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listPayments_bookingNotOwnedByProvider_throwsAccessDenied() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking(PaymentMethod.CASH)));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(OTHER_PROVIDER)));

        assertThatThrownBy(() -> service.listBookingPayments(BOOKING_ID, PROVIDER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listPayments_happyPath_returnsMappedList() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking(PaymentMethod.CASH)));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));
        Payment p = payment(PaymentMethod.CASH, PaymentStatus.COLLECTED);
        when(paymentRepository.findAllByBookingId(BOOKING_ID)).thenReturn(List.of(p));

        List<PaymentResponse> result = service.listBookingPayments(BOOKING_ID, PROVIDER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.COLLECTED);
    }

    // ── approveCashPayment ────────────────────────────────────────────────────

    @Test
    void approveCash_nonCashMethod_throwsBusiness() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking(PaymentMethod.CREDIT_CARD)));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));

        ApproveCashPaymentRequest req = new ApproveCashPaymentRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCurrency("USD");

        assertThatThrownBy(() -> service.approveCashPayment(BOOKING_ID, PROVIDER_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not cash");
    }

    @Test
    void approveCash_alreadyPaid_throwsBusiness() {
        Booking b = booking(PaymentMethod.CASH_ON_ARRIVAL);
        b.markPaid();
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));

        ApproveCashPaymentRequest req = new ApproveCashPaymentRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCurrency("USD");

        assertThatThrownBy(() -> service.approveCashPayment(BOOKING_ID, PROVIDER_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already marked as paid");
    }

    @Test
    void approveCash_happyPath_savesPaymentAndMarksBookingPaid() {
        Booking b = booking(PaymentMethod.CASH_ON_ARRIVAL);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));
        Payment saved = payment(PaymentMethod.CASH, PaymentStatus.COLLECTED);
        when(paymentRepository.save(any())).thenReturn(saved);

        ApproveCashPaymentRequest req = new ApproveCashPaymentRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCurrency("USD");

        service.approveCashPayment(BOOKING_ID, PROVIDER_ID, req);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getPaymentStatus()).isEqualTo(BookingPaymentStatus.PAID);
    }

    // ── recordPayment ─────────────────────────────────────────────────────────

    @Test
    void recordPayment_savesWithStatusRecorded() {
        Booking b = booking(PaymentMethod.BANK_TRANSFER);
        b.setTotalAmount(BigDecimal.valueOf(200));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));
        Payment saved = payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.RECORDED);
        when(paymentRepository.save(any())).thenReturn(saved);
        when(paymentRepository.findAllByBookingId(BOOKING_ID)).thenReturn(List.of(saved));

        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));
        req.setCurrency("USD");
        req.setMethod(PaymentMethod.BANK_TRANSFER);

        PaymentResponse result = service.recordPayment(BOOKING_ID, PROVIDER_ID, req);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.RECORDED);
    }

    @Test
    void recordPayment_whenTotalCovered_marksBookingPaid() {
        Booking b = booking(PaymentMethod.BANK_TRANSFER);
        b.setTotalAmount(BigDecimal.valueOf(100));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));
        Payment saved = payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.RECORDED);
        saved.setAmount(BigDecimal.valueOf(100));
        when(paymentRepository.save(any())).thenReturn(saved);
        when(paymentRepository.findAllByBookingId(BOOKING_ID)).thenReturn(List.of(saved));

        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));
        req.setCurrency("USD");
        req.setMethod(PaymentMethod.BANK_TRANSFER);

        service.recordPayment(BOOKING_ID, PROVIDER_ID, req);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(BookingPaymentStatus.PAID);
    }

    @Test
    void recordPayment_whenNotCovered_doesNotMarkPaid() {
        Booking b = booking(PaymentMethod.BANK_TRANSFER);
        b.setTotalAmount(BigDecimal.valueOf(500));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(PROVIDER_ID)));
        Payment saved = payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.RECORDED);
        saved.setAmount(BigDecimal.valueOf(100));
        when(paymentRepository.save(any())).thenReturn(saved);
        when(paymentRepository.findAllByBookingId(BOOKING_ID)).thenReturn(List.of(saved));

        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));
        req.setCurrency("USD");
        req.setMethod(PaymentMethod.BANK_TRANSFER);

        service.recordPayment(BOOKING_ID, PROVIDER_ID, req);

        verify(bookingRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Booking booking(PaymentMethod method) {
        Booking b = new Booking();
        b.setId(BOOKING_ID);
        b.setServiceId(SERVICE_ID);
        b.setPaymentMethod(method);
        return b;
    }

    private com.ziyara.backend.domain.entity.Service service(UUID providerId) {
        com.ziyara.backend.domain.entity.Service s = new com.ziyara.backend.domain.entity.Service();
        s.setId(SERVICE_ID);
        s.setProviderId(providerId);
        return s;
    }

    private Payment payment(PaymentMethod method, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setBookingId(BOOKING_ID);
        p.setAmount(BigDecimal.TEN);
        p.setCurrency("USD");
        p.setMethod(method);
        p.setStatus(status);
        return p;
    }
}
