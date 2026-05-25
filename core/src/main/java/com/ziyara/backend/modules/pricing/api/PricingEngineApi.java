package com.ziyara.backend.modules.pricing.api;

import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pricing module API (Phase 3 — MODULAR_MONOLITH_STRUCTURE).
 * Booking, portal, and presentation consumers must depend only on this interface.
 * Per PRICING_METHODS: stacked discounts, provider commission, optional tax.
 */
public interface PricingEngineApi {

    PriceBreakdownResponse calculatePrice(PricePreviewRequest request);

    PriceBreakdownResponse calculatePriceInServiceCurrency(UUID serviceId, LocalDate checkIn, LocalDate checkOut,
                                                           int guests, int rooms, String discountCode);
}
