package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.HotelRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelRoomRepository {

    HotelRoom save(HotelRoom room);

    Optional<HotelRoom> findById(UUID id);

    List<HotelRoom> findByServiceId(UUID serviceId);

    Optional<HotelRoom> findByIdAndServiceId(UUID id, UUID serviceId);

    void deleteById(UUID id);
}
