package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.HotelRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelRoomResponse {
    private UUID id;
    private UUID serviceId;
    private String roomType;
    private String roomName;
    private String description;
    private Integer capacity;
    private BigDecimal basePrice;
    private String currency;
    private Integer quantityTotal;
    private Integer quantityAvailable;
    private Map<String, Object> amenities;
    private HotelRoomStatus status;
    private int sortOrder;
    private List<HotelRoomImageResponse> images;
}
