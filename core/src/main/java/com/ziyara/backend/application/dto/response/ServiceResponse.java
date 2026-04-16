package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Service (bookable) response")
public class ServiceResponse {

    private UUID id;
    private UUID providerId;
    private ServiceType type;
    private String name;
    private String description;
    private String location;
    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal basePrice;
    private String currency;
    private ServiceStatus status;
    private Map<String, Object> attributes;
    private Map<String, Object> amenities;
    private String policies;
    private Integer starRating;
    private Integer totalRooms;
    private Integer availableRooms;
    private Integer maxGuests;
    private BigDecimal seasonalMultiplier;
    private BigDecimal taxRate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
