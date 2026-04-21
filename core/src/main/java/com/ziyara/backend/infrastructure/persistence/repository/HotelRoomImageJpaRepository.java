package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HotelRoomImageJpaRepository extends JpaRepository<HotelRoomImageJpaEntity, UUID> {

    List<HotelRoomImageJpaEntity> findByRoomIdOrderByDisplayOrderAscIdAsc(UUID roomId);
}
