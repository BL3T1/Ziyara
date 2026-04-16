package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for price preview (PRICING_METHODS.md).
 * Used to compute breakdown before creating a booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Price preview request")
public class PricePreviewRequest {

    @NotNull
    @Schema(description = "Service ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID serviceId;

    @NotNull
    @FutureOrPresent
    @Schema(description = "Check-in date", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate checkInDate;

    @Future
    @Schema(description = "Check-out date (optional for restaurant/taxi)")
    private LocalDate checkOutDate;

    @Builder.Default
    @Min(1)
    @Max(50)
    @Schema(description = "Number of guests", example = "2")
    private Integer guests = 1;

    @Builder.Default
    @Min(1)
    @Max(20)
    @Schema(description = "Number of rooms", example = "1")
    private Integer rooms = 1;

    @Schema(description = "Discount/promo code (optional)")
    private String discountCode;

    @Builder.Default
    @Size(min = 3, max = 3)
    @Schema(description = "Preferred currency for display", example = "USD")
    private String preferredCurrency = "USD";

    @Schema(description = "Room type ID when discount is restricted by applicable_room_type_ids (hotels/resorts)")
    private UUID roomTypeId;

    @Schema(description = "Ordered menu item IDs for restaurant scope validation")
    private List<UUID> menuItemIds;

    @Schema(description = "Ordered or selected menu section IDs for restaurant scope validation")
    private List<UUID> menuSectionIds;

    public Integer getGuests() {
        return guests != null ? guests : 1;
    }

    public Integer getRooms() {
        return rooms != null ? rooms : 1;
    }
}
