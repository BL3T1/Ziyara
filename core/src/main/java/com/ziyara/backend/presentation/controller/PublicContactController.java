package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.PublicContactRequest;
import com.ziyara.backend.application.service.ContactLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Unauthenticated public endpoints (rate-limited where noted)")
public class PublicContactController {

    private final ContactLeadService contactLeadService;

    @PostMapping("/contact")
    @Operation(summary = "Submit contact form", description = "Stores a marketing/contact lead; same email limited to once per minute.")
    public ResponseEntity<ApiResponse<Void>> contact(
            @Valid @RequestBody PublicContactRequest request,
            HttpServletRequest httpRequest) {
        String ip = resolveClientIp(httpRequest);
        contactLeadService.submit(request, ip);
        return ResponseEntity.ok(ApiResponse.success("Thank you — we received your message.", null));
    }

    private static String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }
}
