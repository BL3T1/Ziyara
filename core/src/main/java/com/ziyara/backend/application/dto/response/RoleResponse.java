package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role with group and permissions")
public class RoleResponse {
    @Schema(description = "Role ID") private UUID id;
    @Schema(description = "Role name") private String name;
    @Schema(description = "Role code") private String code;
    @Schema(description = "Description") private String description;
    @Schema(description = "Hierarchy level") private RoleLevel level;
    @Schema(description = "Group ID") private UUID groupId;
    @Schema(description = "Group name") private String groupName;
    @Schema(description = "System role (not deletable)") private boolean systemRole;
    @Schema(description = "Status") private RoleStatus status;
    @Schema(description = "Permission IDs") private List<UUID> permissionIds;
    @Schema(description = "Permissions (resource:action)") private List<PermissionSummaryResponse> permissions;
    @Schema(description = "Number of users assigned") private long userCount;
    @Schema(description = "Custom sidebar item ids (custom roles); null = use default user-role layout")
    private List<String> navigationItemIds;
    @Schema(description = "Maximum discount percentage this role may approve (0–100)")
    private short maxDiscountPct;
    @Schema(description = "Assignable to provider portal staff")
    private boolean providerRole;
    @Schema(description = "Maximum single payout request amount for this role; null = unlimited")
    private BigDecimal maxPayoutRequestAmount;
}
