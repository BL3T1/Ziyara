package com.ziyara.backend.modules.service.api;

import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.domain.enums.HotelRoomStatus;

import java.util.List;
import java.util.UUID;

public interface HotelRoomApi {
    List<HotelRoomResponse> listByService(UUID serviceId);
    List<HotelRoomResponse> listFiltered(UUID serviceId, Integer floor, String category, HotelRoomStatus status);
    List<Integer> getDistinctFloors(UUID serviceId);
    HotelRoomResponse create(UUID serviceId, CreateHotelRoomRequest request);
    HotelRoomResponse update(UUID serviceId, UUID roomId, UpdateHotelRoomRequest request);
    void delete(UUID serviceId, UUID roomId);
    HotelRoomImageResponse uploadRoomImage(UUID serviceId, UUID roomId, byte[] fileBytes,
                                            String contentType, String originalFilename,
                                            String altText, Boolean primary);
}
