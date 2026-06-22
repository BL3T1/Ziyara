package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.HotelRoom;
import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelRoomMapper {

    public HotelRoom toDomainEntity(HotelRoomJpaEntity entity) {
        if (entity == null) return null;
        HotelRoom room = new HotelRoom();
        room.setId(entity.getId());
        room.setServiceId(entity.getServiceId());
        room.setRoomType(entity.getRoomType());
        room.setRoomName(entity.getRoomName());
        room.setDescription(entity.getDescription());
        room.setCapacity(entity.getCapacity());
        room.setBasePrice(entity.getBasePrice());
        room.setCurrency(entity.getCurrency());
        room.setQuantityTotal(entity.getQuantityTotal());
        room.setQuantityAvailable(entity.getQuantityAvailable());
        room.setAmenities(entity.getAmenities());
        room.setStatus(entity.getStatus());
        room.setSortOrder(entity.getSortOrder());
        room.setCreatedAt(entity.getCreatedAt());
        room.setUpdatedAt(entity.getUpdatedAt());
        return room;
    }

    public HotelRoomJpaEntity toJpaEntity(HotelRoom room) {
        if (room == null) return null;
        return HotelRoomJpaEntity.builder()
                .id(room.getId())
                .serviceId(room.getServiceId())
                .roomType(room.getRoomType())
                .roomName(room.getRoomName())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .currency(room.getCurrency())
                .quantityTotal(room.getQuantityTotal())
                .quantityAvailable(room.getQuantityAvailable())
                .amenities(room.getAmenities())
                .status(room.getStatus())
                .sortOrder(room.getSortOrder())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
