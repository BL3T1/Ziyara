package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.DiscountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to create a discount code")
public class CreateDiscountRequest {

    @NotBlank
    @Schema(description = "Code (unique)", required = true)
    private String code;

    @Schema(description = "Description")
    private String description;

    @NotBlank
    @Schema(description = "Type: PERCENTAGE or FIXED_AMOUNT", required = true)
    private String type;

    @Schema(description = "Value (percentage 0-100 or fixed amount); omit when sponsor = BOTH and use companyValue + providerValue instead")
    private BigDecimal value;

    @Schema(description = "Minimum booking amount")
    private BigDecimal minBookingAmount;

    @Schema(description = "Maximum discount amount (for percentage)")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Start date")
    private LocalDateTime startDate;

    @NotNull
    @Schema(description = "End date (exclusive upper bound for validity)", required = true)
    private LocalDateTime endDate;

    @Schema(description = "Usage limit (0 = unlimited)")
    private Integer usageLimit;

    @Schema(description = "Initial status")
    private DiscountStatus status;

    @Schema(description = "Who funds the discount at checkout: COMPANY, PROVIDER, or BOTH", example = "COMPANY")
    private String sponsor;

    @Schema(description = "Company-side discount amount when sponsor = BOTH")
    private BigDecimal companyValue;

    @Schema(description = "Provider-side discount amount when sponsor = BOTH")
    private BigDecimal providerValue;

    @Schema(description = "Restrict to this provider; null = any provider (company-wide)")
    private UUID providerId;

    @Schema(description = "Restrict to these listing (service) UUIDs; empty = all listings under provider / company")
    private List<UUID> applicableServiceIds;

    @Schema(description = "Restaurant: allowed menu section UUIDs")
    private List<UUID> applicableMenuSectionIds;

    @Schema(description = "Restaurant: allowed menu item UUIDs")
    private List<UUID> applicableMenuItemIds;

    @Schema(description = "Hotel/resort: allowed room-type UUIDs (client must send roomTypeId when booking)")
    private List<UUID> applicableRoomTypeIds;
}
