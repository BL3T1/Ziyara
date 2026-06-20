package com.ziyara.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelRoomImageResponse {
    private UUID id;
    private UUID roomId;
    private String url;
    private String altText;
    private boolean primary;
    private int displayOrder;
}
