package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a service")
public class UpdateServiceRequest {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "City")
    private String city;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Address")
    private String address;

    @Schema(description = "Base price")
    private BigDecimal basePrice;

    @Schema(description = "Status")
    private ServiceStatus status;

    @Schema(description = "Max guests")
    private Integer maxGuests;

    @Schema(description = "Total rooms")
    private Integer totalRooms;

    @Schema(description = "Available rooms")
    private Integer availableRooms;

    @Schema(description = "Star rating")
    private Integer starRating;

    @Schema(description = "Attributes")
    private Map<String, Object> attributes;

    @Schema(description = "Amenities")
    private Map<String, Object> amenities;
}
