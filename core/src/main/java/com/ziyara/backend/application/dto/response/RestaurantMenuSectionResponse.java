package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantMenuSectionResponse {
    private UUID id;
    private UUID serviceId;
    private String title;
    private int sortOrder;
    @Schema(description = "Items in display order")
    private List<RestaurantMenuItemResponse> items;
}
