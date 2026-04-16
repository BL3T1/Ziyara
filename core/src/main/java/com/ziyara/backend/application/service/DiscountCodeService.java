package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.ApplyDiscountRequest;
import com.ziyara.backend.application.dto.request.CreateDiscountRequest;
import com.ziyara.backend.application.dto.request.UpdateDiscountRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.DiscountStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.security.SecurityContextUserId;
import com.ziyara.backend.infrastructure.security.SecurityRoleUtils;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service: DiscountCodeService (Phase 2 – Commands)
 * Handles create, update, delete, approve, deactivate, validate, recordUsage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeService {

    private static final Set<String> ALLOWED_SPONSORS = Set.of("COMPANY", "PROVIDER", "BOTH");

    private final DiscountCodeRepository discountCodeRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final DiscountScopeService discountScopeService;

    @Transactional
    public DiscountResponse create(CreateDiscountRequest request) {
        if (discountCodeRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Discount code already exists: " + request.getCode());
        }
        DiscountCode dc = new DiscountCode();
        dc.setCode(request.getCode());
        dc.setDescription(request.getDescription());
        dc.setType(request.getType());
        dc.setValue(request.getValue());
        dc.setMinBookingAmount(request.getMinBookingAmount() != null ? request.getMinBookingAmount() : BigDecimal.ZERO);
        dc.setMaxDiscountAmount(request.getMaxDiscountAmount());
        dc.setStartDate(request.getStartDate());
        dc.setEndDate(request.getEndDate());
        dc.setUsageLimit(request.getUsageLimit() != null ? request.getUsageLimit() : 0);
        dc.setSponsor(normalizeSponsor(request.getSponsor()));
        dc.setProviderId(request.getProviderId());
        dc.setApplicableServiceIds(emptyToNull(request.getApplicableServiceIds()));
        dc.setApplicableMenuSectionIds(emptyToNull(request.getApplicableMenuSectionIds()));
        dc.setApplicableMenuItemIds(emptyToNull(request.getApplicableMenuItemIds()));
        dc.setApplicableRoomTypeIds(emptyToNull(request.getApplicableRoomTypeIds()));
        validateScopeOnWrite(dc.getProviderId(), dc.getApplicableServiceIds());
        SecurityContextUserId.currentUserId().ifPresent(dc::setCreatedBy);
        if (SecurityRoleUtils.canActivateOrApproveDiscounts()) {
            dc.setStatus(request.getStatus() != null ? request.getStatus() : DiscountStatus.ACTIVE);
        } else {
            dc.setStatus(DiscountStatus.PENDING_APPROVAL);
        }
        DiscountCode saved = discountCodeRepository.save(dc);
        return toResponse(saved);
    }

    @Transactional
    public DiscountResponse update(UUID id, UpdateDiscountRequest request) {
        DiscountCode dc = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        if (request.getDescription() != null) dc.setDescription(request.getDescription());
        if (request.getType() != null) dc.setType(request.getType());
        if (request.getValue() != null) dc.setValue(request.getValue());
        if (request.getMinBookingAmount() != null) dc.setMinBookingAmount(request.getMinBookingAmount());
        if (request.getMaxDiscountAmount() != null) dc.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getStartDate() != null) dc.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) dc.setEndDate(request.getEndDate());
        if (request.getUsageLimit() != null) dc.setUsageLimit(request.getUsageLimit());
        if (request.getSponsor() != null) dc.setSponsor(normalizeSponsor(request.getSponsor()));
        if (request.getProviderId() != null) dc.setProviderId(request.getProviderId());
        if (request.getApplicableServiceIds() != null) dc.setApplicableServiceIds(emptyToNull(request.getApplicableServiceIds()));
        if (request.getApplicableMenuSectionIds() != null) {
            dc.setApplicableMenuSectionIds(emptyToNull(request.getApplicableMenuSectionIds()));
        }
        if (request.getApplicableMenuItemIds() != null) {
            dc.setApplicableMenuItemIds(emptyToNull(request.getApplicableMenuItemIds()));
        }
        if (request.getApplicableRoomTypeIds() != null) {
            dc.setApplicableRoomTypeIds(emptyToNull(request.getApplicableRoomTypeIds()));
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == DiscountStatus.ACTIVE && !SecurityRoleUtils.canActivateOrApproveDiscounts()) {
                throw new IllegalArgumentException("Only Super Admin or CEO can set a discount to active");
            }
            dc.setStatus(request.getStatus());
        }
        validateScopeOnWrite(dc.getProviderId(), dc.getApplicableServiceIds());
        return toResponse(discountCodeRepository.save(dc));
    }

    @Transactional
    public void deleteById(UUID id) {
        if (!discountCodeRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Discount not found");
        }
        discountCodeRepository.deleteById(id);
    }

    @Transactional
    public DiscountResponse approve(UUID id) {
        if (!SecurityRoleUtils.canActivateOrApproveDiscounts()) {
            throw new IllegalArgumentException("Only Super Admin or CEO can approve discounts");
        }
        DiscountCode dc = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        if (dc.getStatus() != DiscountStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only discounts pending approval can be approved");
        }
        dc.setStatus(DiscountStatus.ACTIVE);
        return toResponse(discountCodeRepository.save(dc));
    }

    @Transactional
    public DiscountResponse deactivate(UUID id) {
        DiscountCode dc = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        dc.setStatus(DiscountStatus.INACTIVE);
        return toResponse(discountCodeRepository.save(dc));
    }

    @Transactional(readOnly = true)
    public Optional<DiscountCode> validateCode(ApplyDiscountRequest request, BigDecimal bookingAmount) {
        String code = request.getCode();
        log.info("Validating discount code: {}", code);
        Optional<DiscountCode> base = discountCodeRepository.findByCode(code)
                .filter(dc -> dc.getStatus() == DiscountStatus.ACTIVE)
                .filter(dc -> dc.getEndDate() != null && dc.getEndDate().isAfter(LocalDateTime.now()))
                .filter(dc -> dc.getMinBookingAmount() == null || bookingAmount.compareTo(dc.getMinBookingAmount()) >= 0);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        DiscountCode dc = base.get();
        if (!dc.isValid()) {
            return Optional.empty();
        }
        if (needsServiceContext(dc) && request.getServiceId() == null) {
            return Optional.empty();
        }
        if (request.getServiceId() != null) {
            com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(request.getServiceId()).orElse(null);
            if (svc == null) {
                return Optional.empty();
            }
            try {
                discountScopeService.assertApplicable(
                        dc, svc, request.getRoomTypeId(), request.getMenuItemIds(), request.getMenuSectionIds());
            } catch (BusinessException e) {
                return Optional.empty();
            }
        }
        return Optional.of(dc);
    }

    @Transactional
    public void recordUsage(String code) {
        log.info("Recording usage for discount code: {}", code);
        discountCodeRepository.findByCode(code).ifPresent(dc -> {
            dc.incrementUsage();
            discountCodeRepository.save(dc);
        });
    }

    /**
     * Apply a discount code to a booking. Validates the code, computes discount, updates booking and records usage.
     */
    @Transactional
    public BookingResponse applyToBooking(String code, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Discount can only be applied to PENDING or CONFIRMED bookings");
        }
        DiscountCode dc = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found"));
        if (dc.getStatus() != DiscountStatus.ACTIVE) {
            throw new IllegalArgumentException("Discount code is not active");
        }
        if (!dc.isValid()) {
            throw new IllegalArgumentException("Discount code is not valid or has expired");
        }
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(booking.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        discountScopeService.assertApplicable(
                dc,
                service,
                booking.getDiscountContextRoomTypeId(),
                booking.getDiscountContextMenuItemIds(),
                booking.getDiscountContextMenuSectionIds());
        BigDecimal baseAmount = booking.getBaseAmount() != null ? booking.getBaseAmount() : BigDecimal.ZERO;
        BigDecimal discountAmount = dc.calculateDiscount(baseAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount code is not valid for this booking amount or has expired");
        }
        booking.setDiscountCodeId(dc.getId());
        booking.setDiscountAmount(discountAmount);
        booking.applyDiscount(discountAmount);
        bookingRepository.save(booking);
        recordUsage(code);
        return toBookingResponse(booking);
    }

    private void validateScopeOnWrite(UUID providerId, List<UUID> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return;
        }
        for (UUID sid : serviceIds) {
            com.ziyara.backend.domain.entity.Service s = serviceRepository.findById(sid)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown service id: " + sid));
            if (providerId != null && !providerId.equals(s.getProviderId())) {
                throw new IllegalArgumentException("Service " + sid + " does not belong to the selected provider");
            }
        }
    }

    private static List<UUID> emptyToNull(List<UUID> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }

    private static boolean needsServiceContext(DiscountCode dc) {
        return dc.getProviderId() != null
                || nonEmpty(dc.getApplicableServiceIds())
                || nonEmpty(dc.getApplicableMenuItemIds())
                || nonEmpty(dc.getApplicableMenuSectionIds())
                || nonEmpty(dc.getApplicableRoomTypeIds());
    }

    private static boolean nonEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .customerId(b.getCustomerId())
                .serviceId(b.getServiceId())
                .checkInDate(b.getCheckInDate())
                .checkOutDate(b.getCheckOutDate())
                .guests(b.getGuests())
                .rooms(b.getRooms())
                .baseAmount(b.getBaseAmount())
                .discountAmount(b.getDiscountAmount())
                .taxAmount(b.getTaxAmount())
                .commissionAmount(b.getCommissionAmount())
                .totalAmount(b.getTotalAmount())
                .currency(b.getCurrency())
                .status(b.getStatus())
                .build();
    }

    private static DiscountResponse toResponse(DiscountCode dc) {
        return DiscountResponse.builder()
                .id(dc.getId())
                .code(dc.getCode())
                .description(dc.getDescription())
                .type(dc.getType())
                .value(dc.getValue())
                .minBookingAmount(dc.getMinBookingAmount())
                .maxDiscountAmount(dc.getMaxDiscountAmount())
                .startDate(dc.getStartDate())
                .endDate(dc.getEndDate())
                .usageLimit(dc.getUsageLimit())
                .usageCount(dc.getUsageCount())
                .status(dc.getStatus())
                .createdAt(dc.getCreatedAt())
                .updatedAt(dc.getUpdatedAt())
                .sponsor(dc.getSponsor())
                .providerId(dc.getProviderId())
                .applicableServiceIds(dc.getApplicableServiceIds())
                .applicableMenuSectionIds(dc.getApplicableMenuSectionIds())
                .applicableMenuItemIds(dc.getApplicableMenuItemIds())
                .applicableRoomTypeIds(dc.getApplicableRoomTypeIds())
                .build();
    }

    private static String normalizeSponsor(String raw) {
        if (raw == null || raw.isBlank()) {
            return "COMPANY";
        }
        String u = raw.trim().toUpperCase();
        if (!ALLOWED_SPONSORS.contains(u)) {
            throw new IllegalArgumentException("Invalid discount sponsor; use COMPANY, PROVIDER, or BOTH");
        }
        return u;
    }
}
