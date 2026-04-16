package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.DiscountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a discount code")
public class UpdateDiscountRequest {

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Type: PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @Schema(description = "Value")
    private BigDecimal value;

    @Schema(description = "Minimum booking amount")
    private BigDecimal minBookingAmount;

    @Schema(description = "Maximum discount amount")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Start date")
    private LocalDateTime startDate;

    @Schema(description = "End date")
    private LocalDateTime endDate;

    @Schema(description = "Usage limit")
    private Integer usageLimit;

    @Schema(description = "Status")
    private DiscountStatus status;

    @Schema(description = "COMPANY, PROVIDER, or BOTH")
    private String sponsor;

    @Schema(description = "Restrict to this provider; null clears when explicitly patching (omit field to leave unchanged)")
    private UUID providerId;

    private List<UUID> applicableServiceIds;
    private List<UUID> applicableMenuSectionIds;
    private List<UUID> applicableMenuItemIds;
    private List<UUID> applicableRoomTypeIds;
}
