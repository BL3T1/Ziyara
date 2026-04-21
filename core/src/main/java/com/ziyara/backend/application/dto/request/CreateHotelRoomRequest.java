package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.HotelRoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHotelRoomRequest {

    @NotBlank
    @Size(max = 64)
    private String roomType;

    @NotBlank
    @Size(max = 255)
    private String roomName;

    private String description;

    @NotNull
    @Min(1)
    private Integer capacity;

    private BigDecimal basePrice;

    @Size(max = 3)
    private String currency;

    @NotNull
    @Min(0)
    private Integer quantityTotal;

    @NotNull
    @Min(0)
    private Integer quantityAvailable;

    private Map<String, Object> amenities;

    private HotelRoomStatus status;

    private Integer sortOrder;
}
