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
@Schema(description = "GDPR-style consent record for a user")
public class UserConsentResponse {

    private UUID id;
    private UUID userId;
    private String consentType;
    private String purpose;
    private Boolean granted;
    private LocalDateTime grantedAt;
    private LocalDateTime withdrawnAt;
    private String withdrawalReason;
    private Integer version;
    private String ipAddress;
    private String userAgent;
}
