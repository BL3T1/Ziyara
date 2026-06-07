package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateWebhookSubscriptionRequest;
import com.ziyara.backend.application.dto.response.WebhookDeliveryResponse;
import com.ziyara.backend.application.dto.response.WebhookSubscriptionResponse;
import com.ziyara.backend.application.service.WebhookService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.COMPANY_STAFF;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manage outbound webhook subscriptions (admin-only).
 */
@RestController
@RequestMapping("/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize(COMPANY_STAFF)
@Tag(name = "Webhooks", description = "Outbound webhook subscription management")
@SecurityRequirement(name = "bearerAuth")
public class WebhookSubscriptionController {

    private final WebhookService webhookService;

    @GetMapping
    @Operation(summary = "List subscriptions", description = "All registered webhook subscriptions (paginated)")
    public ResponseEntity<ApiResponse<List<WebhookSubscriptionResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.list(page, size)));
    }

    @PostMapping
    @Operation(summary = "Create subscription", description = "Register a new webhook endpoint. The secret is returned once — store it securely.")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> create(
            @Valid @RequestBody CreateWebhookSubscriptionRequest request) {
        WebhookSubscriptionResponse created = webhookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Webhook subscription created", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subscription", description = "Permanently remove a webhook subscription and its delivery log")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        webhookService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Subscription deleted", null));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Enable or disable subscription")
    public ResponseEntity<ApiResponse<Void>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        webhookService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.success("Subscription updated", null));
    }

    @PostMapping("/{id}/ping")
    @Operation(summary = "Send test ping", description = "POST a test event to the subscription's URL to verify connectivity")
    public ResponseEntity<ApiResponse<Void>> ping(@PathVariable UUID id) {
        webhookService.ping(id);
        return ResponseEntity.ok(ApiResponse.success("Ping dispatched", null));
    }

    @GetMapping("/events")
    @Operation(summary = "Supported events", description = "List event types that can be subscribed to")
    public ResponseEntity<ApiResponse<List<String>>> supportedEvents() {
        return ResponseEntity.ok(ApiResponse.success(webhookService.getSupportedEvents()));
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Delivery log", description = "Recent delivery attempts for a subscription")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryResponse>>> deliveries(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.listDeliveries(id, page, size)));
    }
}
