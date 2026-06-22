package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a service")
public class CreateServiceRequest {

    @Schema(description = "Provider ID (required for /services; injected automatically for /portal/services)")
    private UUID providerId;

    @NotNull
    @Schema(description = "Service type", required = true)
    private ServiceType type;

    @NotBlank
    @Schema(description = "Name", required = true)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "City")
    private String city;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Address")
    private String address;

    @NotNull
    @Schema(description = "Base price", required = true)
    private BigDecimal basePrice;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Max guests")
    private Integer maxGuests;

    @Schema(description = "Total rooms")
    private Integer totalRooms;

    @Schema(description = "Available rooms")
    private Integer availableRooms;

    @Schema(description = "Star rating")
    private Integer starRating;

    @Schema(description = "Attributes (JSON)")
    private Map<String, Object> attributes;

    @Schema(description = "Amenities (JSON)")
    private Map<String, Object> amenities;

    @Schema(description = "Check-in time (e.g. 14:00)")
    private LocalTime checkInTime;

    @Schema(description = "Check-out time (e.g. 11:00)")
    private LocalTime checkOutTime;

    @Schema(description = "Latitude")
    private BigDecimal latitude;

    @Schema(description = "Longitude")
    private BigDecimal longitude;

    @Schema(description = "Cancellation and house policies")
    private String policies;
}
