package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Provider media submission (pending admin approval)")
public class ProviderMediaSubmissionResponse {
    private UUID id;
    private UUID providerId;
    private UUID serviceId;
    @Schema(description = "LOGO, SERVICE, ROOM")
    private String imageType;
    private String contextKey;
    private String fileUrl;
    private String altText;
    private boolean primary;
    @Schema(description = "PENDING, APPROVED, REJECTED")
    private String status;
    private UUID submittedBy;
    private LocalDateTime submittedAt;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;
}
