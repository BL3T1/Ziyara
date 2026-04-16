package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.UserRole;
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
@Schema(description = "Member of a provider portal team")
public class PortalStaffMemberResponse {

    @Schema(description = "Staff link row id; null for the primary owner")
    private UUID staffLinkId;

    @Schema(description = "User id")
    private UUID userId;

    private String email;
    private String phone;
    private UserRole role;
    private String title;
    private boolean owner;
    private LocalDateTime createdAt;
}
