package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.HotelRoom;
import com.ziyara.backend.domain.repository.HotelRoomRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.HotelRoomMapper;
import com.ziyara.backend.infrastructure.persistence.repository.HotelRoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HotelRoomRepositoryAdapter implements HotelRoomRepository {

    private final HotelRoomJpaRepository jpaRepository;
    private final HotelRoomMapper mapper;

    @Override
    public HotelRoom save(HotelRoom room) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(room)));
    }

    @Override
    public Optional<HotelRoom> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<HotelRoom> findByServiceId(UUID serviceId) {
        return jpaRepository.findByServiceIdOrderBySortOrderAscIdAsc(serviceId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HotelRoom> findByIdAndServiceId(UUID id, UUID serviceId) {
        return jpaRepository.findByIdAndServiceId(id, serviceId).map(mapper::toDomainEntity);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
