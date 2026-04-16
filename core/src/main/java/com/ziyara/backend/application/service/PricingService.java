package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.core.api.PricingEngineApi;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Pricing logic per PRICING_METHODS.md. Implements PricingEngineApi (Phase 3).
 * Vertical-specific base; stacked discounts; 10% default commission (provider override); optional tax.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PricingService implements PricingEngineApi {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10");
    private static final int SCALE = 2;

    private final ServiceRepository serviceRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final CurrencyService currencyService;
    private final DiscountScopeService discountScopeService;

    /**
     * Compute price breakdown for a booking (preview or creation).
     * Converts to preferredCurrency when different from service currency.
     */
    @Transactional(readOnly = true)
    public PriceBreakdownResponse calculatePrice(PricePreviewRequest request) {
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        ServiceProvider provider = serviceProviderRepository.findById(service.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        int nights = computeNights(service.getType(), request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal baseAmount = computeBaseAmount(service, request.getGuests(), request.getRooms(), nights);
        BigDecimal seasonalMultiplier = service.getSeasonalMultiplier() != null ? service.getSeasonalMultiplier() : BigDecimal.ONE;
        baseAmount = baseAmount.multiply(seasonalMultiplier).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal providerDiscount = BigDecimal.ZERO;
        BigDecimal companyDiscount = BigDecimal.ZERO;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            Optional<DiscountCode> codeOpt = discountCodeRepository.findByCode(request.getDiscountCode().trim().toUpperCase());
            if (codeOpt.isPresent()) {
                DiscountCode dc = codeOpt.get();
                if (!dc.isValid()) {
                    throw new BusinessException("Discount code is not valid or has expired");
                }
                if (service.getType() == ServiceType.RESTAURANT
                        && request.getMenuItemIds() != null
                        && !request.getMenuItemIds().isEmpty()) {
                    discountScopeService.verifyMenuItemsBelongToService(service.getId(), request.getMenuItemIds());
                }
                discountScopeService.assertApplicable(
                        dc,
                        service,
                        request.getRoomTypeId(),
                        request.getMenuItemIds(),
                        request.getMenuSectionIds());
                BigDecimal fullDiscount = dc.calculateDiscount(baseAmount);
                String sponsor = dc.getSponsor() != null ? dc.getSponsor().trim().toUpperCase() : "COMPANY";
                // BOTH: split 50/50 between provider and company discount buckets (remainder to company).
                switch (sponsor) {
                    case "PROVIDER" -> providerDiscount = fullDiscount;
                    case "BOTH" -> {
                        BigDecimal half = fullDiscount.divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
                        providerDiscount = half;
                        companyDiscount = fullDiscount.subtract(half);
                    }
                    default -> companyDiscount = fullDiscount;
                }
            }
        }

        BigDecimal afterDiscounts = baseAmount.subtract(providerDiscount).subtract(companyDiscount).max(BigDecimal.ZERO);
        BigDecimal commissionRate = provider.getCommissionRate() != null ? provider.getCommissionRate() : DEFAULT_COMMISSION_RATE;
        BigDecimal totalBeforeTax = afterDiscounts.multiply(BigDecimal.ONE.add(commissionRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal taxRate = service.getTaxRate() != null ? service.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = totalBeforeTax.multiply(taxRate).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = totalBeforeTax.add(taxAmount);
        BigDecimal commissionAmount = totalBeforeTax.subtract(afterDiscounts);

        String currency = service.getCurrency() != null ? service.getCurrency() : "USD";
        if (request.getPreferredCurrency() != null && !request.getPreferredCurrency().equalsIgnoreCase(currency)) {
            try {
                baseAmount = currencyService.convert(baseAmount, currency, request.getPreferredCurrency());
                providerDiscount = currencyService.convert(providerDiscount, currency, request.getPreferredCurrency());
                companyDiscount = currencyService.convert(companyDiscount, currency, request.getPreferredCurrency());
                commissionAmount = currencyService.convert(commissionAmount, currency, request.getPreferredCurrency());
                taxAmount = currencyService.convert(taxAmount, currency, request.getPreferredCurrency());
                totalAmount = currencyService.convert(totalAmount, currency, request.getPreferredCurrency());
                currency = request.getPreferredCurrency();
            } catch (Exception e) {
                log.warn("Currency conversion failed, keeping {}", currency, e);
            }
        }

        String pricingModel = pricingModelLabel(service.getType(), nights, request.getRooms(), request.getGuests());

        return PriceBreakdownResponse.builder()
                .baseAmount(baseAmount)
                .providerDiscountAmount(providerDiscount)
                .companyDiscountAmount(companyDiscount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(currency)
                .nights(nights)
                .pricingModel(pricingModel)
                .build();
    }

    /** Same breakdown in service currency (no conversion). Used when creating booking. */
    @Transactional(readOnly = true)
    public PriceBreakdownResponse calculatePriceInServiceCurrency(UUID serviceId, LocalDate checkIn, LocalDate checkOut, int guests, int rooms, String discountCode) {
        PricePreviewRequest req = PricePreviewRequest.builder()
                .serviceId(serviceId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .guests(guests)
                .rooms(rooms)
                .discountCode(discountCode)
                .build();
        return calculatePrice(req);
    }

    private int computeNights(ServiceType type, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        if (type == ServiceType.HOTEL || type == ServiceType.RESORT) {
            if (checkOut == null || !checkOut.isAfter(checkIn)) return 1;
            return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        }
        return 1;
    }

    private BigDecimal computeBaseAmount(com.ziyara.backend.domain.entity.Service service, int guests, int rooms, int nights) {
        BigDecimal base = service.getBasePrice() != null ? service.getBasePrice() : BigDecimal.ZERO;
        switch (service.getType()) {
            case HOTEL:
            case RESORT:
                return base.multiply(new BigDecimal(nights)).multiply(new BigDecimal(rooms));
            case RESTAURANT:
                return base; // flat fee or min spend
            case TRIP:
                return base.multiply(new BigDecimal(guests));
            case TAXI:
            default:
                return base;
        }
    }

    private String pricingModelLabel(ServiceType type, int nights, int rooms, int guests) {
        switch (type) {
            case HOTEL:
            case RESORT:
                return "Per night Ã— " + nights + " nights Ã— " + rooms + " room(s)";
            case RESTAURANT:
                return "Reservation";
            case TRIP:
                return "Per person Ã— " + guests + " guest(s)";
            case TAXI:
            default:
                return "Fixed / dynamic";
        }
    }
}
