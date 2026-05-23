package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.HotelRoomImage;
import com.ziyara.backend.domain.repository.HotelRoomImageRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.HotelRoomImageMapper;
import com.ziyara.backend.infrastructure.persistence.repository.HotelRoomImageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HotelRoomImageRepositoryAdapter implements HotelRoomImageRepository {

    private final HotelRoomImageJpaRepository jpaRepository;
    private final HotelRoomImageMapper mapper;

    @Override
    public HotelRoomImage save(HotelRoomImage image) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(image)));
    }

    @Override
    public List<HotelRoomImage> saveAll(List<HotelRoomImage> images) {
        List<com.ziyara.backend.infrastructure.persistence.entity.HotelRoomImageJpaEntity> entities =
                images.stream().map(mapper::toJpaEntity).collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HotelRoomImage> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<HotelRoomImage> findByRoomId(UUID roomId) {
        return jpaRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(roomId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
