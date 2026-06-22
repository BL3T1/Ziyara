package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateExchangeRateRequest;
import com.ziyara.backend.application.dto.request.UpdateExchangeRateRequest;
import com.ziyara.backend.application.dto.response.ExchangeRateResponse;
import com.ziyara.backend.application.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller: CurrencyController
 * Handles exchange rates and conversion (Phase 3: POST/PUT rates)
 */
@RestController
@RequestMapping("/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Currency conversion and rate APIs")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/rates")
    @Operation(summary = "List all rates", description = "Get all stored exchange rates")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getAllRates() {
        return ResponseEntity.ok(ApiResponse.success(currencyService.getAllRates()));
    }

    @PostMapping("/rates")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(CURRENCY_WRITE)
    @Operation(summary = "Create rate", description = "Create exchange rate (Phase 3)")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> createRate(
            @Valid @RequestBody CreateExchangeRateRequest request) {
        ExchangeRateResponse response = currencyService.createRate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Rate created", response));
    }

    @PatchMapping("/rates/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(CURRENCY_WRITE)
    @Operation(summary = "Update rate", description = "Update exchange rate (Phase 3)")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> updateRate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExchangeRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(currencyService.updateRate(id, request)));
    }

    @GetMapping("/rates/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(CURRENCY_READ)
    @Operation(summary = "Get rate by ID", description = "Single exchange rate row")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getRate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(currencyService.getRate(id)));
    }

    @DeleteMapping("/rates/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(CURRENCY_WRITE)
    @Operation(summary = "Delete rate", description = "Remove an exchange rate row")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable UUID id) {
        currencyService.deleteRate(id);
        return ResponseEntity.ok(ApiResponse.success("Rate deleted", null));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert amount", description = "Convert money between currencies using latest rates")
    public ResponseEntity<ApiResponse<BigDecimal>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return ResponseEntity.ok(ApiResponse.success(currencyService.convert(amount, from, to)));
    }
}
