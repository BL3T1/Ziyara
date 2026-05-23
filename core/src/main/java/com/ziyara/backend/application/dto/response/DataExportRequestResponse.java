package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GDPR data portability export request")
public class DataExportRequestResponse {

    private UUID id;
    private UUID userId;
    private String status;
    private String format;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private Integer recordCount;
    private String failureReason;
}
