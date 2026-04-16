package com.ziyara.backend.application.dto.response;

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
public class RestaurantMenuResponse {
    private UUID serviceId;
    private List<RestaurantMenuSectionResponse> sections;
}
