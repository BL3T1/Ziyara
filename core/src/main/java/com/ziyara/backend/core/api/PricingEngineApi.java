package com.ziyara.backend.core.api;

import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pricing facade (Phase 3). Booking and payment modules use this for price breakdown and commission.
 * Per PRICING_METHODS: stacked discounts, provider commission, optional tax.
 */
public interface PricingEngineApi {

    PriceBreakdownResponse calculatePrice(PricePreviewRequest request);

    PriceBreakdownResponse calculatePriceInServiceCurrency(UUID serviceId, LocalDate checkIn, LocalDate checkOut,
                                                           int guests, int rooms, String discountCode);
}
