package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.response.TaxiBookingResponse;
import com.ziyarah.application.service.TaxiBookingService;
import com.ziyarah.domain.enums.TaxiStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: TaxiBookingController
 * Handles taxi-specific workflow management
 */
@RestController
@RequestMapping("/taxi-bookings")
@RequiredArgsConstructor
@Tag(name = "Taxi Bookings", description = "Taxi-specific operation APIs")
@SecurityRequirement(name = "bearerAuth")
public class TaxiBookingController {
    
    private final TaxiBookingService taxiBookingService;
    
    @GetMapping("/active")
    @Operation(summary = "List active trips", description = "Retrieve all trips currently assigned to drivers")
    public ResponseEntity<ApiResponse<List<TaxiBookingResponse>>> getActiveTrips() {
        return ResponseEntity.ok(ApiResponse.success(taxiBookingService.getActiveBookings()));
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update trip status", description = "Update the real-time status of a taxi trip")
    public ResponseEntity<ApiResponse<TaxiBookingResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam TaxiStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(taxiBookingService.updateTaxiStatus(id, status)));
    }
    
    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign driver", description = "Assign a driver and vehicle to a taxi booking")
    public ResponseEntity<ApiResponse<TaxiBookingResponse>> assignDriver(
            @PathVariable UUID id,
            @RequestParam UUID driverId,
            @RequestParam String driverName,
            @RequestParam String plate,
            @RequestParam String model
    ) {
        TaxiBookingResponse response = taxiBookingService.assignDriver(id, driverId, driverName, plate, model);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned", response));
    }
}
