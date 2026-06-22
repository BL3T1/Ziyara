package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.HotelRoomImage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelRoomImageRepository {

    HotelRoomImage save(HotelRoomImage image);

    List<HotelRoomImage> saveAll(List<HotelRoomImage> images);

    Optional<HotelRoomImage> findById(UUID id);

    List<HotelRoomImage> findByRoomId(UUID roomId);

    void deleteById(UUID id);

    int clearPrimaryByRoomId(UUID roomId);
}
