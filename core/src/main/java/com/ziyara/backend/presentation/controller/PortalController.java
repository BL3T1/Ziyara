package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.ApproveCashPaymentRequest;
import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreatePortalDiscountRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.PayoutRequestPayload;
import com.ziyara.backend.application.dto.request.RecordPaymentRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.PayoutRequestResponse;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.PortalDiscountBalanceResponse;
import com.ziyara.backend.application.dto.response.PortalEarningsResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.dto.request.MarkRoomOccupiedRequest;
import com.ziyara.backend.application.service.MapService;
import com.ziyara.backend.application.service.PortalPaymentService;
import com.ziyara.backend.application.service.PortalService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.application.service.WalkInConflictService;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ziyara.backend.application.annotation.RateLimit;
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
    private final PortalPaymentService portalPaymentService;
    private final ServiceProviderService providerService;
    private final MapService mapService;
    private final WalkInConflictService walkInConflictService;
    private final MediaStorageService mediaStorageService;

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
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Create service", description = "Create a new service listing for current provider")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(@Valid @RequestBody CreateServiceRequest request) {
        UUID providerId = requireCurrentProviderId();
        ServiceResponse response = portalService.createService(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service created", response));
    }

    @PatchMapping("/services/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Update service", description = "Update own service listing")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateService(providerId, id, request)));
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
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

    @PatchMapping("/services/{id}/images/{imageId}")
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

    @RateLimit(key = "portal-image-upload", maxPerMinute = 20)
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
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Create menu section", description = "RESTAURANT listings only")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> createMenuSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuSectionRequest request) {
        UUID providerId = requireCurrentProviderId();
        RestaurantMenuSectionResponse created = portalService.createMenuSection(providerId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Section created", created));
    }

    @PatchMapping("/services/{id}/menu/sections/{sectionId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Update menu section", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> updateMenuSection(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateMenuSectionRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateMenuSection(providerId, id, sectionId, request)));
    }

    @DeleteMapping("/services/{id}/menu/sections/{sectionId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Delete menu section", description = "Own listing only")
    public ResponseEntity<ApiResponse<Void>> deleteMenuSection(@PathVariable UUID id, @PathVariable UUID sectionId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteMenuSection(providerId, id, sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section deleted", null));
    }

    @PostMapping("/services/{id}/menu/sections/{sectionId}/items")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Create menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> createMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        UUID providerId = requireCurrentProviderId();
        RestaurantMenuItemResponse created = portalService.createMenuItem(providerId, id, sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Item created", created));
    }

    @PatchMapping("/services/{id}/menu/items/{itemId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Update menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateMenuItem(providerId, id, itemId, request)));
    }

    @DeleteMapping("/services/{id}/menu/items/{itemId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Delete menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteMenuItem(providerId, id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }

    // ── Hotel Rooms ───────────────────────────────────────────────────────────

    @GetMapping("/services/{id}/rooms")
    @Operation(summary = "List rooms for own hotel/resort service")
    public ResponseEntity<ApiResponse<List<HotelRoomResponse>>> getHotelRooms(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getHotelRooms(providerId, id)));
    }

    @PostMapping("/services/{id}/rooms")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Create room for own hotel/resort service")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> createHotelRoom(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHotelRoomRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created", portalService.createHotelRoom(providerId, id, request)));
    }

    @PatchMapping("/services/{id}/rooms/{roomId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Update room for own hotel/resort service")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> updateHotelRoom(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateHotelRoomRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateHotelRoom(providerId, id, roomId, request)));
    }

    @DeleteMapping("/services/{id}/rooms/{roomId}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Delete room from own hotel/resort service")
    public ResponseEntity<ApiResponse<Void>> deleteHotelRoom(@PathVariable UUID id, @PathVariable UUID roomId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteHotelRoom(providerId, id, roomId);
        return ResponseEntity.ok(ApiResponse.success("Room deleted", null));
    }

    @PostMapping(value = "/services/{id}/rooms/{roomId}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Upload image for a room")
    public ResponseEntity<ApiResponse<HotelRoomImageResponse>> uploadRoomImage(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) Boolean primary) {
        UUID providerId = requireCurrentProviderId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room image uploaded",
                portalService.uploadRoomImage(providerId, id, roomId, bytes,
                        file.getContentType(), file.getOriginalFilename(), altText, primary)));
    }

    // ── Menu item image ───────────────────────────────────────────────────────

    @PostMapping(value = "/services/{id}/menu/items/{itemId}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MENU_MANAGE)
    @Operation(summary = "Upload image for a menu item")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> uploadMenuItemImage(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestParam("file") MultipartFile file) {
        UUID providerId = requireCurrentProviderId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Menu item image uploaded",
                portalService.uploadMenuItemImage(providerId, id, itemId, bytes,
                        file.getContentType(), file.getOriginalFilename())));
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_BOOKINGS_READ)
    @Operation(summary = "My bookings", description = "List bookings for current provider's services")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getBookings(providerId)));
    }

    @GetMapping("/bookings/{id}/payments")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_BOOKINGS_READ)
    @Operation(summary = "List payments for a booking")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listBookingPayments(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalPaymentService.listBookingPayments(id, providerId)));
    }

    @PostMapping("/bookings/{id}/payments/cash-approve")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Approve a cash payment for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> approveCashPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveCashPaymentRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalPaymentService.approveCashPayment(id, providerId, request)));
    }

    @PostMapping("/bookings/{id}/payments")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Record a manual payment for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded", portalPaymentService.recordPayment(id, providerId, request)));
    }

    @GetMapping("/earnings")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_REPORTS_READ)
    @Operation(summary = "My earnings", description = "Total completed payment amount for provider's bookings")
    public ResponseEntity<ApiResponse<PortalEarningsResponse>> getEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getEarnings(providerId, start, end)));
    }

    @GetMapping("/earnings/export")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_REPORTS_READ)
    @Operation(summary = "Download earnings as CSV")
    public ResponseEntity<byte[]> exportEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID providerId = requireCurrentProviderId();
        PortalEarningsResponse earnings = portalService.getEarnings(providerId, start, end);
        byte[] csv = portalService.buildEarningsCsv(earnings);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"earnings.csv\"");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    // ── Payout requests ───────────────────────────────────────────────────────

    @GetMapping("/payout-requests")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "List payout requests for current provider")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<PayoutRequestResponse>>> listPayoutRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.listPayoutRequests(providerId, page, size)));
    }

    @RateLimit(key = "portal-payout-request", maxPerMinute = 3)
    @PostMapping("/payout-request")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Submit a payout request")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> createPayoutRequest(
            @Valid @RequestBody PayoutRequestPayload payload) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payout request submitted", portalService.createPayoutRequest(providerId, payload)));
    }

    // ── Provider discounts ────────────────────────────────────────────────────

    @GetMapping("/discount-balance")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Get provider discount balance")
    public ResponseEntity<ApiResponse<PortalDiscountBalanceResponse>> getDiscountBalance() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getDiscountBalance(providerId)));
    }

    @GetMapping("/discounts")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "List provider-issued discount codes")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DiscountResponse>>> listDiscounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.listProviderDiscounts(providerId, page, size)));
    }

    @RateLimit(key = "portal-discount-create", maxPerMinute = 10)
    @PostMapping("/discounts")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Create a provider-funded discount code")
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(
            @Valid @RequestBody CreatePortalDiscountRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discount created", portalService.createProviderDiscount(providerId, request)));
    }

    @DeleteMapping("/discounts/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
    @Operation(summary = "Deactivate a provider discount code")
    public ResponseEntity<ApiResponse<Void>> deactivateDiscount(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        portalService.deactivateProviderDiscount(providerId, id);
        return ResponseEntity.ok(ApiResponse.success("Discount deactivated", null));
    }

    // ── Walk-in conflict / room filters ─────────────────────────────────────

    @PostMapping("/services/{id}/rooms/{roomId}/mark-occupied")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_BOOKINGS_MANAGE)
    @Operation(summary = "Mark a room as occupied by a walk-in guest, cancelling overlapping bookings")
    public ResponseEntity<ApiResponse<WalkInConflictService.WalkInResult>> markRoomOccupied(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @Valid @RequestBody MarkRoomOccupiedRequest request) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        var result = walkInConflictService.markRoomOccupied(
                roomId, id, providerId,
                request.getCheckInDate(), request.getCheckOutDate(),
                request.getReason(), userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/services/{id}/rooms/floors")
    @Operation(summary = "List distinct floor numbers for a service's rooms")
    public ResponseEntity<ApiResponse<List<Integer>>> getRoomFloors(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getRoomFloors(providerId, id)));
    }

    @GetMapping("/services/{id}/rooms/filtered")
    @Operation(summary = "List rooms with optional floor/category/status filters")
    public ResponseEntity<ApiResponse<List<HotelRoomResponse>>> getFilteredRooms(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(
                portalService.getFilteredRooms(providerId, id, floor, category, status)));
    }

    // ── Provider profile edit (approval workflow) ──────────────────────────

    @PutMapping("/profile")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Submit profile edit request (goes through approval workflow)")
    public ResponseEntity<ApiResponse<com.ziyara.backend.domain.entity.ProviderProfileEditRequest>> submitProfileEdit(
            @RequestBody java.util.Map<String, Object> newValues) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        var editRequest = portalService.submitProfileEditRequest(providerId, userId, newValues);
        return ResponseEntity.ok(ApiResponse.success("Profile edit submitted for approval", editRequest));
    }

    @GetMapping("/profile/edit-status")
    @Operation(summary = "Check pending edit request status")
    public ResponseEntity<ApiResponse<com.ziyara.backend.domain.entity.ProviderProfileEditRequest>> getEditStatus() {
        UUID providerId = requireCurrentProviderId();
        var pending = portalService.getPendingEditRequest(providerId);
        if (pending == null) {
            return ResponseEntity.ok(ApiResponse.success("No pending edit request", null));
        }
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    // ── Provider logo upload ────────────────────────────────────────────────

    @PostMapping(value = "/profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Upload provider logo")
    public ResponseEntity<ApiResponse<String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        UUID providerId = requireCurrentProviderId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        String url = mediaStorageService.storeProviderImage(providerId, bytes,
                file.getContentType(), file.getOriginalFilename());
        portalService.updateProviderLogo(providerId, url);
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded", url));
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
