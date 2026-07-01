package com.ziyara.backend.application.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JourneyRecommendationResponse {
    private List<ServiceResponse> hotels;
    private List<ServiceResponse> taxis;
    private List<ServiceResponse> restaurants;
}
