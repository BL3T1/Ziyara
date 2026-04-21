package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.domain.enums.HotelRoomStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.media.LocalMediaStorageService;
import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomImageJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.HotelRoomJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.HotelRoomImageJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.HotelRoomJpaRepository;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelRoomService {

    private final ServiceRepository serviceRepository;
    private final HotelRoomJpaRepository roomRepository;
    private final HotelRoomImageJpaRepository roomImageRepository;
    private final LocalMediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public List<HotelRoomResponse> listByService(UUID serviceId) {
        requireHotelService(serviceId);
        return roomRepository.findByServiceIdOrderBySortOrderAscIdAsc(serviceId).stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HotelRoomResponse create(UUID serviceId, CreateHotelRoomRequest request) {
        requireHotelService(serviceId);
        validateQuantities(request.getQuantityTotal(), request.getQuantityAvailable());
        HotelRoomJpaEntity saved = roomRepository.save(HotelRoomJpaEntity.builder()
                .serviceId(serviceId)
                .roomType(request.getRoomType().trim())
                .roomName(request.getRoomName().trim())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .basePrice(request.getBasePrice())
                .currency(trimOrNull(request.getCurrency()))
                .quantityTotal(request.getQuantityTotal())
                .quantityAvailable(request.getQuantityAvailable())
                .amenities(request.getAmenities())
                .status(request.getStatus() != null ? request.getStatus() : HotelRoomStatus.ACTIVE)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build());
        recalculateServiceRoomTotals(serviceId);
        return toRoomResponse(saved);
    }

    @Transactional
    public HotelRoomResponse update(UUID serviceId, UUID roomId, UpdateHotelRoomRequest request) {
        requireHotelService(serviceId);
        HotelRoomJpaEntity room = roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (request.getRoomType() != null) room.setRoomType(request.getRoomType().trim());
        if (request.getRoomName() != null) room.setRoomName(request.getRoomName().trim());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getCapacity() != null) room.setCapacity(request.getCapacity());
        if (request.getBasePrice() != null) room.setBasePrice(request.getBasePrice());
        if (request.getCurrency() != null) room.setCurrency(trimOrNull(request.getCurrency()));
        if (request.getQuantityTotal() != null) room.setQuantityTotal(request.getQuantityTotal());
        if (request.getQuantityAvailable() != null) room.setQuantityAvailable(request.getQuantityAvailable());
        if (request.getAmenities() != null) room.setAmenities(request.getAmenities());
        if (request.getStatus() != null) room.setStatus(request.getStatus());
        if (request.getSortOrder() != null) room.setSortOrder(request.getSortOrder());
        validateQuantities(room.getQuantityTotal(), room.getQuantityAvailable());
        HotelRoomJpaEntity saved = roomRepository.save(room);
        recalculateServiceRoomTotals(serviceId);
        return toRoomResponse(saved);
    }

    @Transactional
    public void delete(UUID serviceId, UUID roomId) {
        requireHotelService(serviceId);
        HotelRoomJpaEntity room = roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        roomRepository.delete(room);
        recalculateServiceRoomTotals(serviceId);
    }

    @Transactional
    public HotelRoomImageResponse uploadRoomImage(
            UUID serviceId,
            UUID roomId,
            byte[] fileBytes,
            String contentType,
            String originalFilename,
            String altText,
            Boolean primary) {
        requireHotelService(serviceId);
        HotelRoomJpaEntity room = roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (Boolean.TRUE.equals(primary)) {
            clearPrimaryImage(room.getId());
        }
        String url = mediaStorageService.storeServiceImage(serviceId, fileBytes, contentType, originalFilename);
        int order = roomImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(room.getId()).size();
        HotelRoomImageJpaEntity image = roomImageRepository.save(HotelRoomImageJpaEntity.builder()
                .roomId(room.getId())
                .url(url)
                .altText(trimOrNull(altText))
                .primary(Boolean.TRUE.equals(primary))
                .displayOrder(order)
                .build());
        return toImageResponse(image);
    }

    @Transactional(readOnly = true)
    public List<HotelRoomImageResponse> listRoomImages(UUID serviceId, UUID roomId) {
        requireHotelService(serviceId);
        roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return roomImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(roomId).stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    private void clearPrimaryImage(UUID roomId) {
        List<HotelRoomImageJpaEntity> existing = roomImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(roomId);
        for (HotelRoomImageJpaEntity image : existing) {
            if (Boolean.TRUE.equals(image.getPrimary())) {
                image.setPrimary(false);
                roomImageRepository.save(image);
            }
        }
    }

    private void recalculateServiceRoomTotals(UUID serviceId) {
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        List<HotelRoomJpaEntity> rooms = roomRepository.findByServiceIdOrderBySortOrderAscIdAsc(serviceId);
        int total = rooms.stream().map(HotelRoomJpaEntity::getQuantityTotal).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        int available = rooms.stream().map(HotelRoomJpaEntity::getQuantityAvailable).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        service.setTotalRooms(total);
        service.setAvailableRooms(available);
        serviceRepository.save(service);
    }

    private com.ziyara.backend.domain.entity.Service requireHotelService(UUID serviceId) {
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (service.getType() != ServiceType.HOTEL) {
            throw new BusinessException("Room inventory is only available for HOTEL services");
        }
        return service;
    }

    private void validateQuantities(Integer total, Integer available) {
        if (total == null || available == null || total < 0 || available < 0 || available > total) {
            throw new BusinessException("Invalid room quantities");
        }
    }

    private HotelRoomResponse toRoomResponse(HotelRoomJpaEntity room) {
        return HotelRoomResponse.builder()
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
                .amenities(room.getAmenities() != null ? room.getAmenities() : Map.of())
                .status(room.getStatus())
                .sortOrder(room.getSortOrder() != null ? room.getSortOrder() : 0)
                .images(roomImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(room.getId()).stream()
                        .map(this::toImageResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private HotelRoomImageResponse toImageResponse(HotelRoomImageJpaEntity image) {
        return HotelRoomImageResponse.builder()
                .id(image.getId())
                .roomId(image.getRoomId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .primary(Boolean.TRUE.equals(image.getPrimary()))
                .displayOrder(image.getDisplayOrder() != null ? image.getDisplayOrder() : 0)
                .build();
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
