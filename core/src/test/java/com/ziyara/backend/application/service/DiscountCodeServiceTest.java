package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.ApplyDiscountRequest;
import com.ziyara.backend.application.dto.request.CreateDiscountRequest;
import com.ziyara.backend.application.dto.request.UpdateDiscountRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.enums.DiscountStatus;
import com.ziyara.backend.domain.enums.DiscountType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.modules.webhook.api.WebhookEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountCodeServiceTest {

    @Mock DiscountCodeRepository discountCodeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock DiscountScopeService discountScopeService;
    @Mock WebhookEventPublisher webhookEventPublisher;

    @InjectMocks DiscountCodeService discountCodeService;

    private static final UUID DISCOUNT_ID = UUID.randomUUID();

    private DiscountCode activeDiscount;

    @BeforeEach
    void setUp() {
        activeDiscount = new DiscountCode();
        activeDiscount.setId(DISCOUNT_ID);
        activeDiscount.setCode("SAVE10");
        activeDiscount.setType(DiscountType.PERCENTAGE);
        activeDiscount.setValue(new BigDecimal("10.00"));
        activeDiscount.setStatus(DiscountStatus.ACTIVE);
        activeDiscount.setStartDate(LocalDateTime.now().minusDays(1));
        activeDiscount.setEndDate(LocalDateTime.now().plusDays(30));
        activeDiscount.setUsageLimit(100);
        activeDiscount.setMinBookingAmount(BigDecimal.ZERO);
        activeDiscount.setSponsor("COMPANY");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void duplicateCode_throwsIllegalArgument() {
            when(discountCodeRepository.existsByCode("SAVE10")).thenReturn(true);

            CreateDiscountRequest request = buildCreateRequest("SAVE10");

            assertThatThrownBy(() -> discountCodeService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(discountCodeRepository, never()).save(any());
        }

        @Test
        void validRequest_savesAndReturnsResponse() {
            when(discountCodeRepository.existsByCode("NEW20")).thenReturn(false);
            when(discountCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateDiscountRequest request = buildCreateRequest("NEW20");
            DiscountResponse response = discountCodeService.create(request);

            verify(discountCodeRepository).save(any());
            assertThat(response.getCode()).isEqualTo("NEW20");
        }

        @Test
        void invalidSponsor_throwsIllegalArgument() {
            when(discountCodeRepository.existsByCode("CODE")).thenReturn(false);

            CreateDiscountRequest request = buildCreateRequest("CODE");
            request.setSponsor("INVALID");

            assertThatThrownBy(() -> discountCodeService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid discount sponsor");
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    class Update {

        @Test
        void notFound_throwsResourceNotFound() {
            when(discountCodeRepository.findById(DISCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountCodeService.update(DISCOUNT_ID, new UpdateDiscountRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void found_updatesAndReturns() {
            when(discountCodeRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(activeDiscount));
            when(discountCodeRepository.save(any())).thenReturn(activeDiscount);

            UpdateDiscountRequest request = new UpdateDiscountRequest();
            request.setDescription("Updated desc");

            DiscountResponse response = discountCodeService.update(DISCOUNT_ID, request);

            verify(discountCodeRepository).save(any());
        }
    }

    // ── deleteById ────────────────────────────────────────────────────────────

    @Test
    void deleteById_found_callsRepository() {
        when(discountCodeRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(activeDiscount));

        discountCodeService.deleteById(DISCOUNT_ID);

        verify(discountCodeRepository).deleteById(DISCOUNT_ID);
    }

    @Test
    void deleteById_notFound_throwsResourceNotFound() {
        when(discountCodeRepository.findById(DISCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountCodeService.deleteById(DISCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void deactivate_setsInactive() {
        when(discountCodeRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(activeDiscount));
        when(discountCodeRepository.save(any())).thenReturn(activeDiscount);

        DiscountResponse response = discountCodeService.deactivate(DISCOUNT_ID);

        assertThat(activeDiscount.getStatus()).isEqualTo(DiscountStatus.INACTIVE);
    }

    // ── validateCode ──────────────────────────────────────────────────────────

    @Nested
    class ValidateCode {

        @Test
        void validCode_returnsDiscount() {
            when(discountCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activeDiscount));

            ApplyDiscountRequest request = new ApplyDiscountRequest();
            request.setCode("SAVE10");

            Optional<DiscountResponse> result = discountCodeService.validateCode(request, new BigDecimal("200.00"));

            assertThat(result).isPresent();
        }

        @Test
        void inactiveCode_returnsEmpty() {
            activeDiscount.setStatus(DiscountStatus.INACTIVE);
            when(discountCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activeDiscount));

            ApplyDiscountRequest request = new ApplyDiscountRequest();
            request.setCode("SAVE10");

            Optional<DiscountResponse> result = discountCodeService.validateCode(request, new BigDecimal("200.00"));

            assertThat(result).isEmpty();
        }

        @Test
        void expiredCode_returnsEmpty() {
            activeDiscount.setEndDate(LocalDateTime.now().minusDays(1));
            when(discountCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(activeDiscount));

            ApplyDiscountRequest request = new ApplyDiscountRequest();
            request.setCode("SAVE10");

            Optional<DiscountResponse> result = discountCodeService.validateCode(request, new BigDecimal("200.00"));

            assertThat(result).isEmpty();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateDiscountRequest buildCreateRequest(String code) {
        CreateDiscountRequest req = new CreateDiscountRequest();
        req.setCode(code);
        req.setType(DiscountType.PERCENTAGE);
        req.setValue(new BigDecimal("10.00"));
        req.setStartDate(LocalDateTime.now().minusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(30));
        req.setSponsor("COMPANY");
        return req;
    }
}
