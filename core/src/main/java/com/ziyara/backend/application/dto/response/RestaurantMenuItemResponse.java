package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantMenuItemResponse {
    private UUID id;
    private UUID sectionId;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private int sortOrder;
}
