package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.JourneyRecommendationResponse;
import com.ziyara.backend.application.service.JourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/journeys")
@RequiredArgsConstructor
@Tag(name = "Journeys", description = "Airport-arrival journey recommendations")
public class JourneyController {

    private final JourneyService journeyService;

    @GetMapping("/recommend")
    @Operation(summary = "Recommend hotel, taxi, and restaurant for an arriving traveller")
    public ResponseEntity<ApiResponse<JourneyRecommendationResponse>> recommend(
            @RequestParam String city,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) BigDecimal maxBudget) {
        return ResponseEntity.ok(ApiResponse.success(
            "Recommendations ready",
            journeyService.recommend(city, guests, maxBudget)
        ));
    }
}
