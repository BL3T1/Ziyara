package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to apply a discount code")
public class ApplyDiscountRequest {
    
    @NotBlank(message = "Discount code is required")
    @Schema(description = "The code to be applied")
    private String code;

    @Schema(description = "Service (listing) ID — required when the code is provider- or listing-scoped")
    private UUID serviceId;

    @Schema(description = "Room type ID for hotel/resort scoped codes")
    private UUID roomTypeId;

    @Schema(description = "Menu item IDs for restaurant scoped codes")
    private List<UUID> menuItemIds;

    @Schema(description = "Menu section IDs for restaurant scoped codes")
    private List<UUID> menuSectionIds;
}
