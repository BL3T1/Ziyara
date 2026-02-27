package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.response.ExchangeRateResponse;
import com.ziyarah.application.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller: CurrencyController
 * Handles public exchange rates and conversion
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
