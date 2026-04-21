package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.response.NotificationInboxResponse;
import com.ziyara.backend.application.dto.response.NotificationResponse;
import com.ziyara.backend.application.service.NotificationService;
import com.ziyara.backend.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller: NotificationController
 * Handles notification delivery and user preferences
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final JwtService jwtService;
    
    @GetMapping
    @Operation(summary = "Get user notifications (paged)", description = "Notifications for the authenticated user; includes unreadCount for badge")
    public ResponseEntity<ApiResponse<NotificationInboxResponse>> getMyNotifications(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotificationsInbox(userId, page, size)));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create notification", description = "Send a manual notification (Super Admin only)")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification queued successfully", response));
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader
    ) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for current user (Phase 3)")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification", description = "Get notification by ID (ownership enforced)")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        NotificationResponse response = notificationService.getNotification(id, userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return UUID.fromString(jwtService.extractUserId(token));
    }
}
