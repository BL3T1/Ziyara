package com.ziyara.backend.application.service;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountApprovalServiceTest {

    @Mock DiscountCodeRepository discountCodeRepository;

    DiscountApprovalService service;

    @BeforeEach
    void setUp() {
        service = new DiscountApprovalService(discountCodeRepository);
    }

    // ── submitForApproval ─────────────────────────────────────────────────────

    @Test
    void submitForApproval_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(discountCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitForApproval(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submitForApproval_notDraft_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        DiscountCode code = discount(id, "PENDING_APPROVAL");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.submitForApproval(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void submitForApproval_draft_changesStatusToPending() {
        UUID id = UUID.randomUUID();
        DiscountCode code = discount(id, "DRAFT");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));
        when(discountCodeRepository.save(any())).thenReturn(code);

        service.submitForApproval(id);

        assertThat(code.getApprovalStatus()).isEqualTo("PENDING_APPROVAL");
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_notPendingApproval_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        DiscountCode code = discount(id, "DRAFT");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.approve(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_APPROVAL");
    }

    @Test
    void approve_pendingApproval_approvesWithApproverAndTimestamp() {
        UUID id = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        DiscountCode code = discount(id, "PENDING_APPROVAL");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));
        when(discountCodeRepository.save(any())).thenReturn(code);

        service.approve(id, approverId);

        assertThat(code.getApprovalStatus()).isEqualTo("APPROVED");
        assertThat(code.getApprovedBy()).isEqualTo(approverId);
        assertThat(code.getApprovedAt()).isNotNull();
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    void reject_notPendingApproval_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        DiscountCode code = discount(id, "APPROVED");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.reject(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_APPROVAL");
    }

    @Test
    void reject_pendingApproval_rejectsWithRejectorAndTimestamp() {
        UUID id = UUID.randomUUID();
        UUID rejectorId = UUID.randomUUID();
        DiscountCode code = discount(id, "PENDING_APPROVAL");
        when(discountCodeRepository.findById(id)).thenReturn(Optional.of(code));
        when(discountCodeRepository.save(any())).thenReturn(code);

        service.reject(id, rejectorId);

        assertThat(code.getApprovalStatus()).isEqualTo("REJECTED");
        assertThat(code.getApprovedBy()).isEqualTo(rejectorId);
        assertThat(code.getApprovedAt()).isNotNull();
    }

    private DiscountCode discount(UUID id, String approvalStatus) {
        DiscountCode c = new DiscountCode();
        c.setId(id);
        c.setCode("SAVE10");
        c.setApprovalStatus(approvalStatus);
        return c;
    }
}
