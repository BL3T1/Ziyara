package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.HotelRoomImage;
import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomImageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelRoomImageMapper {

    public HotelRoomImage toDomainEntity(HotelRoomImageJpaEntity entity) {
        if (entity == null) return null;
        HotelRoomImage image = new HotelRoomImage();
        image.setId(entity.getId());
        image.setRoomId(entity.getRoomId());
        image.setUrl(entity.getUrl());
        image.setAltText(entity.getAltText());
        image.setPrimary(entity.getPrimary());
        image.setDisplayOrder(entity.getDisplayOrder());
        image.setCreatedAt(entity.getCreatedAt());
        image.setUpdatedAt(entity.getUpdatedAt());
        return image;
    }

    public HotelRoomImageJpaEntity toJpaEntity(HotelRoomImage image) {
        if (image == null) return null;
        return HotelRoomImageJpaEntity.builder()
                .id(image.getId())
                .roomId(image.getRoomId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .primary(image.getPrimary())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
