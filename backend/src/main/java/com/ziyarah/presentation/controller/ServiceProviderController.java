package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.CreateServiceProviderRequest;
import com.ziyarah.application.dto.request.UpdateServiceProviderRequest;
import com.ziyarah.application.dto.response.ServiceProviderResponse;
import com.ziyarah.application.service.ServiceProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: ServiceProviderController
 * Handles provider registration and profiles
 */
@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
@Tag(name = "Service Providers", description = "Provider management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ServiceProviderController {
    
    private final ServiceProviderService providerService;
    
    @GetMapping
    @Operation(summary = "List providers", description = "Retrieve all registered service providers")
    public ResponseEntity<ApiResponse<List<ServiceProviderResponse>>> getAllProviders() {
        return ResponseEntity.ok(ApiResponse.success(providerService.getAllProviders()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get provider", description = "Retrieve provider details by ID")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> getProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProvider(id)));
    }
    
    @PostMapping
    @Operation(summary = "Register provider", description = "Register as a new service provider")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> registerProvider(
            @Valid @RequestBody CreateServiceProviderRequest request
    ) {
        ServiceProviderResponse response = providerService.createProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Provider registered successfully", response));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update provider", description = "Update provider profile details")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> updateProvider(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceProviderRequest request
    ) {
        ServiceProviderResponse response = providerService.updateProvider(id, request);
        return ResponseEntity.ok(ApiResponse.success("Provider updated successfully", response));
    }
}
