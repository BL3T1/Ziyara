package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.ApproveCashPaymentRequest;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.RecordPaymentRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.service.PortalPaymentService;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreatePortalDiscountRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.PayoutRequestPayload;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.dto.response.PayoutRequestResponse;
import com.ziyara.backend.application.dto.response.PortalDiscountBalanceResponse;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.PortalEarningsResponse;
import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.application.service.ProviderMediaSubmissionService;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.service.PortalService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
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
import org.springframework.web.multipart.MultipartFile;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.annotation.RateLimit;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Provider portal: dashboard, my services, my bookings, earnings (BACKEND_CRUD_REPORT Â§4).
 * All endpoints require authenticated user to be a registered provider.
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@PreAuthorize(PROVIDER_PORTAL)
@Tag(name = "Provider Portal", description = "Provider-scoped dashboard, services, bookings, earnings")
@SecurityRequirement(name = "bearerAuth")
public class PortalController {

    private final PortalService portalService;
    private final ServiceProviderService providerService;
    private final ProviderMediaSubmissionService mediaSubmissionService;
    private final PortalPaymentService portalPaymentService;

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

    @GetMapping("/services/{id}")
    @Operation(summary = "Get own service", description = "Get a single service listing owned by the current provider")
    public ResponseEntity<ApiResponse<ServiceResponse>> getService(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getService(providerId, id)));
    }

    @PostMapping("/services")
    @Operation(summary = "Create service", description = "Create a new service listing for current provider")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(@Valid @RequestBody CreateServiceRequest request) {
        UUID providerId = requireCurrentProviderId();
        ServiceResponse response = portalService.createService(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service created", response));
    }

    @PatchMapping("/services/{id}")
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

    @PatchMapping("/services/{id}/menu/sections/{sectionId}")
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

    @PatchMapping("/services/{id}/menu/items/{itemId}")
    @Operation(summary = "Update menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateMenuItem(providerId, id, itemId, request)));
    }

    @PostMapping(value = "/services/{id}/menu/items/{itemId}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload menu item image", description = "RESTAURANT listings only")
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
        RestaurantMenuItemResponse updated = portalService.uploadMenuItemImage(
                providerId, id, itemId, bytes, file.getContentType(), file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.success("Item image uploaded", updated));
    }

    @DeleteMapping("/services/{id}/menu/items/{itemId}")
    @Operation(summary = "Delete menu item", description = "Own listing only")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteMenuItem(providerId, id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }

    @GetMapping("/services/{id}/rooms")
    @Operation(summary = "List hotel rooms", description = "HOTEL listings only")
    public ResponseEntity<ApiResponse<List<HotelRoomResponse>>> listHotelRooms(@PathVariable UUID id) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getHotelRooms(providerId, id)));
    }

    @PostMapping("/services/{id}/rooms")
    @Operation(summary = "Create hotel room", description = "HOTEL listings only")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> createHotelRoom(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHotelRoomRequest request) {
        UUID providerId = requireCurrentProviderId();
        HotelRoomResponse created = portalService.createHotelRoom(providerId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room created", created));
    }

    @PatchMapping("/services/{id}/rooms/{roomId}")
    @Operation(summary = "Update hotel room", description = "HOTEL listings only")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> updateHotelRoom(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateHotelRoomRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.updateHotelRoom(providerId, id, roomId, request)));
    }

    @DeleteMapping("/services/{id}/rooms/{roomId}")
    @Operation(summary = "Delete hotel room", description = "HOTEL listings only")
    public ResponseEntity<ApiResponse<Void>> deleteHotelRoom(@PathVariable UUID id, @PathVariable UUID roomId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deleteHotelRoom(providerId, id, roomId);
        return ResponseEntity.ok(ApiResponse.success("Room deleted", null));
    }

    @PostMapping(value = "/services/{id}/rooms/{roomId}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload room image", description = "HOTEL listings only")
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
        HotelRoomImageResponse created = portalService.uploadRoomImage(
                providerId,
                id,
                roomId,
                bytes,
                file.getContentType(),
                file.getOriginalFilename(),
                altText,
                primary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room image uploaded", created));
    }

    @GetMapping("/bookings")
    @Operation(summary = "My bookings", description = "List bookings for current provider's services")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getBookings(providerId)));
    }

    @GetMapping("/bookings/{bookingId}/payments")
    @Operation(summary = "List payments for a booking")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listBookingPayments(@PathVariable UUID bookingId) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalPaymentService.listBookingPayments(bookingId, providerId)));
    }

    @PostMapping("/bookings/{bookingId}/payments/cash-approve")
    @PreAuthorize(PORTAL_FINANCE)
    @Operation(summary = "Approve cash payment collection for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> approveCashPayment(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ApproveCashPaymentRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(portalPaymentService.approveCashPayment(bookingId, providerId, request)));
    }

    @PostMapping("/bookings/{bookingId}/payments")
    @PreAuthorize(PORTAL_FINANCE)
    @Operation(summary = "Record an offline/manual payment for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RecordPaymentRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(portalPaymentService.recordPayment(bookingId, providerId, request)));
    }

    @GetMapping("/payout-requests")
    @Operation(summary = "Payout request history", description = "List the signed-in provider's payout requests, newest first")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<PayoutRequestResponse>>> listPayoutRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.listPayoutRequests(providerId, page, size)));
    }

    @PostMapping("/payout-request")
    @RateLimit(key = "POST:/portal/payout-request", maxPerMinute = 3)
    @Operation(summary = "Request payout", description = "Provider submits a withdrawal request for ops team to process")
    public ResponseEntity<ApiResponse<PayoutRequestResponse>> requestPayout(
            @Valid @RequestBody PayoutRequestPayload payload) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payout request submitted", portalService.createPayoutRequest(providerId, payload)));
    }

    @GetMapping("/earnings")
    @Operation(summary = "My earnings", description = "Earnings summary with profit-share breakdown and per-service detail")
    public ResponseEntity<ApiResponse<PortalEarningsResponse>> getEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getEarnings(providerId, start, end)));
    }

    @GetMapping("/earnings/export")
    @Operation(summary = "Export my earnings", description = "Download earnings breakdown as CSV")
    public ResponseEntity<byte[]> exportEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID providerId = requireCurrentProviderId();
        PortalEarningsResponse earnings = portalService.getEarnings(providerId, start, end);
        byte[] csv = portalService.buildEarningsCsv(earnings);
        String startLabel = start != null ? start.toString() : "all";
        String endLabel   = end   != null ? end.toString()   : "now";
        String filename   = String.format("earnings-%s-to-%s.csv", startLabel, endLabel);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PostMapping(value = "/services/{id}/images/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit service image for approval", description = "Upload an image for a service listing — requires admin approval before going live")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> submitServiceImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String imageType,
            @RequestParam(required = false, defaultValue = "false") Boolean primary) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        ProviderMediaSubmissionResponse created = mediaSubmissionService.submitServiceImage(
                providerId, id, bytes, file.getContentType(), file.getOriginalFilename(),
                imageType, altText, Boolean.TRUE.equals(primary), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image submitted for approval", created));
    }

    @PostMapping(value = "/logo/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit provider logo for approval", description = "Upload a new logo — requires admin approval before going live")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> submitProviderLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        ProviderMediaSubmissionResponse created = mediaSubmissionService.submitProviderLogo(
                providerId, bytes, file.getContentType(), file.getOriginalFilename(), altText, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Logo submitted for approval", created));
    }

    @GetMapping("/media-submissions")
    @Operation(summary = "List own media submissions", description = "Get all image submissions for current provider with their status")
    public ResponseEntity<ApiResponse<List<ProviderMediaSubmissionResponse>>> getMySubmissions() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(mediaSubmissionService.getProviderSubmissions(providerId)));
    }

    // ── Self-discount engine ──────────────────────────────────────────────

    @GetMapping("/discount-balance")
    @Operation(summary = "Discount balance", description = "Current provider's self-discount balance (allocated / spent / available)")
    public ResponseEntity<ApiResponse<PortalDiscountBalanceResponse>> getDiscountBalance() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.getDiscountBalance(providerId)));
    }

    @GetMapping("/discounts")
    @Operation(summary = "My discounts", description = "List self-funded discount codes for the current provider")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DiscountResponse>>> listDiscounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalService.listProviderDiscounts(providerId, page, size)));
    }

    @PostMapping("/discounts")
    @RateLimit(key = "POST:/portal/discounts", maxPerMinute = 10)
    @Operation(summary = "Create self-discount", description = "Create a provider-funded discount code, debiting from your balance")
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(
            @Valid @RequestBody CreatePortalDiscountRequest request) {
        UUID providerId = requireCurrentProviderId();
        DiscountResponse created = portalService.createProviderDiscount(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Discount submitted for review", created));
    }

    @DeleteMapping("/discounts/{discountId}")
    @Operation(summary = "Deactivate self-discount", description = "Deactivate own discount code")
    public ResponseEntity<ApiResponse<Void>> deactivateDiscount(@PathVariable UUID discountId) {
        UUID providerId = requireCurrentProviderId();
        portalService.deactivateProviderDiscount(providerId, discountId);
        return ResponseEntity.ok(ApiResponse.success("Discount deactivated", null));
    }

    // ─────────────────────────────────────────────────────────────────────────

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
