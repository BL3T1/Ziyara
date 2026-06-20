package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HotelRoomJpaRepository extends JpaRepository<HotelRoomJpaEntity, UUID> {

    List<HotelRoomJpaEntity> findByServiceIdOrderBySortOrderAscIdAsc(UUID serviceId);

    Optional<HotelRoomJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);
}
