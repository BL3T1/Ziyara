package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.application.dto.response.ServiceAvailabilityResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.application.service.RestaurantMenuService;
import com.ziyara.backend.application.service.HotelRoomService;
import com.ziyara.backend.application.service.ReviewService;
import com.ziyara.backend.application.service.ServiceImageService;
import com.ziyara.backend.application.service.ServiceService;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Controller: ServiceController (Phase 2)
 * GET list/search/by-id = jOOQ; POST/PUT/DELETE = ServiceService.
 */
@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Bookable services CRUD and search")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {

    private static final Logger log = LoggerFactory.getLogger(ServiceController.class);

    private final ServiceService serviceService;
    private final ServiceQueryHandler serviceQueryHandler;
    private final ServiceImageService serviceImageService;
    private final RestaurantMenuService restaurantMenuService;
    private final HotelRoomService hotelRoomService;
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "List services", description = "Paginated list with optional filters")
    public ResponseEntity<ApiResponse<Page<ServiceResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    serviceQueryHandler.findPage(page, size, providerId, type, status, city, country)));
        } catch (Exception e) {
            log.warn("Services list query failed (returning empty page): {}", e.getMessage());
            Page<ServiceResponse> empty = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(page, size),
                    0);
            return ResponseEntity.ok(ApiResponse.success(empty));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search services", description = "Search by name/description, type, city, price range")
    public ResponseEntity<ApiResponse<Page<ServiceResponse>>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    serviceQueryHandler.search(page, size, q, type, city, minPrice, maxPrice)));
        } catch (Exception e) {
            log.warn("Services search query failed (returning empty page): {}", e.getMessage());
            Page<ServiceResponse> empty = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(page, size),
                    0);
            return ResponseEntity.ok(ApiResponse.success(empty));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service", description = "Get service by ID")
    public ResponseEntity<ApiResponse<ServiceResponse>> getById(@PathVariable UUID id) {
        try {
            return serviceQueryHandler.findById(id)
                    .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Service get by id failed: {}", e.getMessage());
            throw new ResourceNotFoundException("Service not found");
        }
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check availability", description = "Check if service is available for optional date and nights")
    public ResponseEntity<ApiResponse<ServiceAvailabilityResponse>> getAvailability(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, defaultValue = "1") int nights) {
        return ResponseEntity.ok(ApiResponse.success(serviceService.checkAvailability(id, date, nights)));
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "List service images", description = "Get all images for a service (ordered)")
    public ResponseEntity<ApiResponse<List<ServiceImageResponse>>> getImages(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(serviceImageService.list(id)));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Add service image", description = "Add image by URL (company staff)")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody CreateServiceImageRequest request) {
        ServiceImageResponse created = serviceImageService.create(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image added", created));
    }

    @PatchMapping("/{id}/images/{imageId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Update service image", description = "Partial update (company staff)")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> updateImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateServiceImageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(serviceImageService.update(id, imageId, request)));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Delete service image", description = "Remove an image (company staff)")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        serviceImageService.delete(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }

    @PostMapping(value = "/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Upload service image", description = "Multipart image file (JPEG/PNG/WebP/GIF, max 10MB); stored under app.media.storage-root")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contextKey,
            @RequestParam(required = false) Boolean primary) {
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        ServiceImageResponse created = serviceImageService.uploadAndCreateImage(
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

    /**
     * Public endpoint — returns PUBLISHED / APPROVED reviews for a service.
     * Deliberately kept separate from the staff-only /reviews/service/{id} which returns all statuses.
     */
    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get service reviews (public)", description = "Returns published/approved reviews for a service. No authentication required.")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getServiceReviews(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getServiceReviews(id)));
    }

    @GetMapping("/{id}/menu")
    @Operation(summary = "Get restaurant menu", description = "Sections and items; empty for non-restaurant services")
    public ResponseEntity<ApiResponse<RestaurantMenuResponse>> getMenu(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(restaurantMenuService.getMenu(id)));
    }

    @GetMapping("/{id}/rooms")
    @Operation(summary = "List hotel rooms", description = "HOTEL services only")
    public ResponseEntity<ApiResponse<List<HotelRoomResponse>>> listRooms(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(hotelRoomService.listByService(id)));
    }

    @PostMapping("/{id}/rooms")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Create hotel room", description = "HOTEL services only (company staff)")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> createRoom(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHotelRoomRequest request) {
        HotelRoomResponse created = hotelRoomService.create(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room created", created));
    }

    @PatchMapping("/{id}/rooms/{roomId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Update hotel room", description = "HOTEL services only (company staff)")
    public ResponseEntity<ApiResponse<HotelRoomResponse>> updateRoom(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateHotelRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hotelRoomService.update(id, roomId, request)));
    }

    @DeleteMapping("/{id}/rooms/{roomId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Delete hotel room", description = "HOTEL services only (company staff)")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id, @PathVariable UUID roomId) {
        hotelRoomService.delete(id, roomId);
        return ResponseEntity.ok(ApiResponse.success("Room deleted", null));
    }

    @PostMapping(value = "/{id}/rooms/{roomId}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Upload room image", description = "HOTEL services only")
    public ResponseEntity<ApiResponse<HotelRoomImageResponse>> uploadRoomImage(
            @PathVariable UUID id,
            @PathVariable UUID roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) Boolean primary) {
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        HotelRoomImageResponse created = hotelRoomService.uploadRoomImage(
                id, roomId, bytes, file.getContentType(), file.getOriginalFilename(), altText, primary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Room image uploaded", created));
    }

    @PostMapping("/{id}/menu/sections")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Create menu section", description = "RESTAURANT services only (company staff)")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> createMenuSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuSectionRequest request) {
        RestaurantMenuSectionResponse created = restaurantMenuService.createSection(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Section created", created));
    }

    @PatchMapping("/{id}/menu/sections/{sectionId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Update menu section", description = "Company staff")
    public ResponseEntity<ApiResponse<RestaurantMenuSectionResponse>> updateMenuSection(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateMenuSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(restaurantMenuService.updateSection(id, sectionId, request)));
    }

    @DeleteMapping("/{id}/menu/sections/{sectionId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Delete menu section", description = "Deletes section and its items (company staff)")
    public ResponseEntity<ApiResponse<Void>> deleteMenuSection(@PathVariable UUID id, @PathVariable UUID sectionId) {
        restaurantMenuService.deleteSection(id, sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section deleted", null));
    }

    @PostMapping("/{id}/menu/sections/{sectionId}/items")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Create menu item", description = "Company staff")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> createMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        RestaurantMenuItemResponse created = restaurantMenuService.createItem(id, sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Item created", created));
    }

    @PatchMapping("/{id}/menu/items/{itemId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Update menu item", description = "Company staff")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(restaurantMenuService.updateItem(id, itemId, request)));
    }

    @PostMapping(value = "/{id}/menu/items/{itemId}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Upload menu item image", description = "RESTAURANT services only (company staff)")
    public ResponseEntity<ApiResponse<RestaurantMenuItemResponse>> uploadMenuItemImage(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestParam("file") MultipartFile file) {
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read uploaded file");
        }
        RestaurantMenuItemResponse updated = restaurantMenuService.uploadItemImage(
                id, itemId, bytes, file.getContentType(), file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.success("Item image uploaded", updated));
    }

    @DeleteMapping("/{id}/menu/items/{itemId}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Delete menu item", description = "Company staff")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        restaurantMenuService.deleteItem(id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }

    @PostMapping
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Create service", description = "Create a new bookable service (company staff; providers use /portal/services)")
    public ResponseEntity<ApiResponse<ServiceResponse>> create(@Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = serviceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service created", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Update service", description = "Update service details (company staff; providers use /portal/services)")
    public ResponseEntity<ApiResponse<ServiceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(serviceService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(SERVICES_PUBLISH)
    @Operation(summary = "Approve service", description = "Set service status to ACTIVE — requires services:publish permission")
    public ResponseEntity<ApiResponse<ServiceResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Service approved", serviceService.approve(id)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize(SERVICES_PUBLISH)
    @Operation(summary = "Suspend service", description = "Set service status to SUSPENDED — requires services:publish permission")
    public ResponseEntity<ApiResponse<ServiceResponse>> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Service suspended", serviceService.suspend(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Delete service", description = "Soft-delete a service (company staff; providers use /portal/services)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        serviceService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
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
