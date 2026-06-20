package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.BookingPaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPaymentStatusTest {

    @Test
    void initialStatus_isUnpaid() {
        assertThat(new Booking().getPaymentStatus()).isEqualTo(BookingPaymentStatus.UNPAID);
    }

    @Test
    void markPaid_setsStatusPaid() {
        Booking b = new Booking();
        b.markPaid();
        assertThat(b.getPaymentStatus()).isEqualTo(BookingPaymentStatus.PAID);
    }

    @Test
    void markPaid_updatesTimestamp() {
        Booking b = new Booking();
        b.markPaid();
        assertThat(b.getUpdatedAt()).isNotNull();
    }

    @Test
    void isPaymentPending_trueWhenUnpaid() {
        assertThat(new Booking().isPaymentPending()).isTrue();
    }

    @Test
    void isPaymentPending_falseAfterMarkPaid() {
        Booking b = new Booking();
        b.markPaid();
        assertThat(b.isPaymentPending()).isFalse();
    }

    @Test
    void markPartiallyPaid_setsStatusPartiallyPaid() {
        Booking b = new Booking();
        b.markPartiallyPaid();
        assertThat(b.getPaymentStatus()).isEqualTo(BookingPaymentStatus.PARTIALLY_PAID);
    }

    @Test
    void isPaymentPending_trueWhenPartiallyPaid() {
        Booking b = new Booking();
        b.markPartiallyPaid();
        assertThat(b.isPaymentPending()).isTrue();
    }
}
