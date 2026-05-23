package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to attach a seat-expansion add-on to an existing subscription")
public class AddSubscriptionAddOnRequest {

    @NotBlank(message = "Add-on code is required")
    @Schema(description = "Product code identifying the add-on, e.g. EXTRA_SEATS_10",
            example = "EXTRA_SEATS_10")
    private String addOnCode;

    @NotBlank(message = "Display name is required")
    @Schema(description = "Human-readable name shown in billing UI",
            example = "10 Extra Seats")
    private String displayName;

    @Min(value = 1, message = "Extra seats must be at least 1")
    @Schema(description = "Number of additional seats this add-on grants", example = "10")
    private int extraSeats;

    @NotNull(message = "Price is required")
    @Schema(description = "Amount charged for this add-on", example = "49.99")
    private BigDecimal price;
}
