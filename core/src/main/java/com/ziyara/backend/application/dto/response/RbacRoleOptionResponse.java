package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Minimal custom role row for RBAC assignment UI (HR / Super Admin)")
public class RbacRoleOptionResponse {
    private UUID id;
    private String code;
    private String name;
}
