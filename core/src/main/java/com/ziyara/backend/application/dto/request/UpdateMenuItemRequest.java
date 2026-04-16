package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMenuItemRequest {

    @Size(max = 255)
    private String name;

    private String description;

    private BigDecimal price;

    @Size(max = 3)
    private String currency;

    @Size(max = 500)
    private String imageUrl;

    private Integer sortOrder;
}
