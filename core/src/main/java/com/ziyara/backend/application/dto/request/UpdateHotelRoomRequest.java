package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.HotelRoomStatus;
import jakarta.validation.constraints.Min;
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
public class UpdateHotelRoomRequest {

    @Size(max = 64)
    private String roomType;

    @Size(max = 255)
    private String roomName;

    private String description;

    @Min(1)
    private Integer capacity;

    private BigDecimal basePrice;

    @Size(max = 3)
    private String currency;

    @Min(0)
    private Integer quantityTotal;

    @Min(0)
    private Integer quantityAvailable;

    private Map<String, Object> amenities;

    private HotelRoomStatus status;

    private Integer sortOrder;

    private Integer floorNumber;

    @Size(max = 32)
    private String roomCategory;

    @Size(max = 16)
    private String bedType;

    private BigDecimal areaSqm;

    @Size(max = 16)
    private String viewType;

    private Boolean smokingAllowed;

    private Boolean isAccessible;
}
