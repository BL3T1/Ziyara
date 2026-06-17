package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.PortalEarningsResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.service.MapService;
import com.ziyara.backend.application.service.PortalService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ziyara.backend.application.exception.BusinessException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Provider portal: dashboard, my services, my bookings, earnings (BACKEND_CRUD_REPORT §4).
 * All endpoints require authenticated user to be a registered provider.
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
@Tag(name = "Provider Portal", description = "Provider-scoped dashboard, services, bookings, earnings")
@SecurityRequirement(name = "bearerAuth")
public class PortalController {

    private final PortalService portalService;
    private final ServiceProviderService providerService;
    private final MapService mapService;

    @GetMapping("/map/pins")
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
    @Operation(summary = "Provider's own listing locations (portal map)")
    public ResponseEntity<ApiResponse<List<ProviderMapPinResponse>>> getPortalMapPins() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(mapService.getPortalPins(providerId)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Portal dashboard", description = "KPIs for current provider (services, bookings, revenue)")
    public ResponseEntity<ApiResponse<PortalDashboardResponse>> getDashboard() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getDashboard(providerId)));
    }

    @GetMapping("/services")
    @Operation(summary = "My services", description = "List current provider's service listings")
    public ResponseEntity<ApiResponse<Page<ServiceResponse>>> getServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getServices(providerId, page, size)));
    }

    @PostMapping("/services")
    @Operation(summary = "Create service", description = "Create a new service listing for current provider")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(@Valid @RequestBody CreateServiceRequest request) {
        UUID providerId = requireCurrentProviderId();
        ServiceResponse response = portalService.createService(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service created", response));
    }

    @PutMapping("/services/{id}")
    @Operation(summary = "Update service", description = "Update own service listing")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateService(providerId, id, request)));
    }

    @DeleteMapping("/services/{id}")
    @Operation(summary = "Delete service", description = "Soft-delete own service listing")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteService(providerId, id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
    }

    @GetMapping("/services/{id}/images")
    @Operation(summary = "List images for own service", description = "Same payload as GET /services/{id}/images")
    public ResponseEntity<ApiResponse<List<ServiceImageResponse>>> getServiceImages(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getServiceImages(providerId, id)));
    }

    @PostMapping("/services/{id}/images")
    @Operation(summary = "Add image to own service", description = "URL-based image (provider)")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> addServiceImage(
            @PathVariable UUID id,
            @Valid @RequestBody CreateServiceImageRequest request) {
        UUID providerId = requireCurrentProviderId();
        ServiceImageResponse created = portalService.createServiceImage(providerId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image added", created));
    }

    @PutMapping("/services/{id}/images/{imageId}")
    @Operation(summary = "Update own service image", description = "Partial update")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> updateServiceImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateServiceImageRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateServiceImage(providerId, id, imageId, request)));
    }

    @DeleteMapping("/services/{id}/images/{imageId}")
    @Operation(summary = "Delete own service image", description = "Remove image from listing")
    public ResponseEntity<ApiResponse<Void>> deleteServiceImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteServiceImage(providerId, id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }

    @PostMapping(value = "/services/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image to own service", description = "Multipart image (JPEG/PNG/WebP/GIF, max 10MB)")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> uploadServiceImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contextKey,
            @RequestParam(required = false) Boolean primary) {
        UUID providerId = requireCurrentProviderId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        ServiceImageResponse created = portalService.uploadServiceImage(
                providerId,
                id,
                bytes,
                file.getContentType(),
                file.getOriginalFilename(),
                altText,
                parseImageCategory(category),
                contextKey,
                primary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image uploaded", created));
    }

    @GetMapping("/services/{id}/menu")
    @Operation(summary = "Get restaurant menu for own service", description = "Empty sections if not a restaurant")
    public ResponseEntity<ApiResponse<RestaurantMenuResponse>> getRestaurantMenu(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getRestaurantMenu(providerId, id)));
    }

    @PostMapping("/services/{id}/menu/sections")
    @Operation(summary = "Create menu section", description = "RESTAURANT listings only")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> createMenuSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuSectionRequest request) {
        UUID providerId = requireCurrentProviderId();
        RestaurantMenuSectionResponse created = portalService.createMenuSection(providerId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Section created", created));
    }

    @PutMapping("/services/{id}/menu/sections/{sectionId}")
    @Operation(summary = "Update menu section", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> updateMenuSection(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateMenuSectionRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateMenuSection(providerId, id, sectionId, request)));
    }

    @DeleteMapping("/services/{id}/menu/sections/{sectionId}")
    @Operation(summary = "Delete menu section", description = "Own listing only")
    public ResponseEntity<ApiResponse<Void>> deleteMenuSection(@PathVariable UUID id, @PathVariable UUID sectionId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteMenuSection(providerId, id, sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section deleted", null));
    }

    @PostMapping("/services/{id}/menu/sections/{sectionId}/items")
    @Operation(summary = "Create menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> createMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        UUID providerId = requireCurrentProviderId();
        RestaurantMenuItemResponse created = portalService.createMenuItem(providerId, id, sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Item created", created));
    }

    @PutMapping("/services/{id}/menu/items/{itemId}")
    @Operation(summary = "Update menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateMenuItem(providerId, id, itemId, request)));
    }

    @DeleteMapping("/services/{id}/menu/items/{itemId}")
    @Operation(summary = "Delete menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteMenuItem(providerId, id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }

    @GetMapping("/bookings")
    @Operation(summary = "My bookings", description = "List bookings for current provider's services")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getBookings(providerId)));
    }

    @GetMapping("/earnings")
    @Operation(summary = "My earnings", description = "Total completed payment amount for provider's bookings")
    public ResponseEntity<ApiResponse<PortalEarningsResponse>> getEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getEarnings(providerId, start, end)));
    }

    private UUID requireCurrentProviderId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return providerService.getProviderByUserId(userId)
                .map(p -> p.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No provider profile for this user"));
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private static ServiceImageCategory parseImageCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return ServiceImageCategory.PROPERTY;
        }
        try {
            return ServiceImageCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: " + raw);
        }
    }
}
