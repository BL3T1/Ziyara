package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.modules.pricing.api.PricingEngineApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Price preview per PRICING_METHODS.md.
 */
@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Price preview and breakdown APIs")
@SecurityRequirement(name = "bearerAuth")
public class PricingController {

    private final PricingEngineApi pricingService;

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get price breakdown", description = "Preview price with discounts and commission before booking")
    public ResponseEntity<ApiResponse<PriceBreakdownResponse>> preview(@Valid @RequestBody PricePreviewRequest request) {
        PriceBreakdownResponse breakdown = pricingService.calculatePrice(request);
        return ResponseEntity.ok(ApiResponse.success(breakdown));
    }
}
