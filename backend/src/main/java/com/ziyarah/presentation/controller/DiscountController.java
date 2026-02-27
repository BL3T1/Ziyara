package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.ApplyDiscountRequest;
import com.ziyarah.application.service.DiscountCodeService;
import com.ziyarah.domain.entity.DiscountCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller: DiscountController
 * Handles coupon validation and redemption
 */
@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Promotion and coupon APIs")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {
    
    private final DiscountCodeService discountService;
    
    @PostMapping("/validate")
    @Operation(summary = "Validate code", description = "Check if a discount code is valid for a given amount")
    public ResponseEntity<ApiResponse<DiscountCode>> validateCode(
            @Valid @RequestBody ApplyDiscountRequest request,
            @RequestParam BigDecimal amount
    ) {
        return discountService.validateCode(request.getCode(), amount)
                .map(dc -> ResponseEntity.ok(ApiResponse.success("Code is valid", dc)))
                .orElseThrow(() -> new RuntimeException("Invalid or expired discount code"));
    }
}
