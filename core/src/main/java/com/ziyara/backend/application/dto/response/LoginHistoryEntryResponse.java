package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO: LoginHistoryEntryResponse
 * Single login event (from last_login_at / last_login_ip on users, or audit_log LOGIN entries).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login history entry")
public class LoginHistoryEntryResponse {

    @Schema(description = "Login timestamp")
    private LocalDateTime loginAt;

    @Schema(description = "Client IP address")
    private String ipAddress;
}
