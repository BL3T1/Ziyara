package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusPortalTest {

    @Test
    void collected_isPortalRecorded() {
        assertThat(PaymentStatus.COLLECTED.isPortalRecorded()).isTrue();
    }

    @Test
    void recorded_isPortalRecorded() {
        assertThat(PaymentStatus.RECORDED.isPortalRecorded()).isTrue();
    }

    @Test
    void completed_isNotPortalRecorded() {
        assertThat(PaymentStatus.COMPLETED.isPortalRecorded()).isFalse();
    }

    @Test
    void pending_isNotPortalRecorded() {
        assertThat(PaymentStatus.PENDING.isPortalRecorded()).isFalse();
    }
}
