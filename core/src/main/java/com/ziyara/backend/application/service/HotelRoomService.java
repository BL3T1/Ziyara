package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.domain.entity.HotelRoom;
import com.ziyara.backend.domain.entity.HotelRoomImage;
import com.ziyara.backend.domain.enums.HotelRoomStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.HotelRoomImageRepository;
import com.ziyara.backend.domain.repository.HotelRoomRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
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
    private final HotelRoomRepository roomRepository;
    private final HotelRoomImageRepository roomImageRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public List<HotelRoomResponse> listByService(UUID serviceId) {
        requireHotelService(serviceId);
        return roomRepository.findByServiceId(serviceId).stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HotelRoomResponse create(UUID serviceId, CreateHotelRoomRequest request) {
        requireHotelService(serviceId);
        validateQuantities(request.getQuantityTotal(), request.getQuantityAvailable());
        HotelRoom room = new HotelRoom();
        room.setServiceId(serviceId);
        room.setRoomType(request.getRoomType().trim());
        room.setRoomName(request.getRoomName().trim());
        room.setDescription(request.getDescription());
        room.setCapacity(request.getCapacity());
        room.setBasePrice(request.getBasePrice());
        room.setCurrency(trimOrNull(request.getCurrency()));
        room.setQuantityTotal(request.getQuantityTotal());
        room.setQuantityAvailable(request.getQuantityAvailable());
        room.setAmenities(request.getAmenities());
        room.setStatus(request.getStatus() != null ? request.getStatus() : HotelRoomStatus.ACTIVE);
        room.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        HotelRoom saved = roomRepository.save(room);
        recalculateServiceRoomTotals(serviceId);
        return toRoomResponse(saved);
    }

    @Transactional
    public HotelRoomResponse update(UUID serviceId, UUID roomId, UpdateHotelRoomRequest request) {
        requireHotelService(serviceId);
        HotelRoom room = roomRepository.findByIdAndServiceId(roomId, serviceId)
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
        HotelRoom saved = roomRepository.save(room);
        recalculateServiceRoomTotals(serviceId);
        return toRoomResponse(saved);
    }

    @Transactional
    public void delete(UUID serviceId, UUID roomId) {
        requireHotelService(serviceId);
        roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        roomRepository.deleteById(roomId);
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
        HotelRoom room = roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (Boolean.TRUE.equals(primary)) {
            clearPrimaryImage(room.getId());
        }
        String url = mediaStorageService.storeServiceImage(serviceId, fileBytes, contentType, originalFilename);
        int order = roomImageRepository.findByRoomId(room.getId()).size();
        HotelRoomImage image = new HotelRoomImage();
        image.setRoomId(room.getId());
        image.setUrl(url);
        image.setAltText(trimOrNull(altText));
        image.setPrimary(Boolean.TRUE.equals(primary));
        image.setDisplayOrder(order);
        return toImageResponse(roomImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<HotelRoomImageResponse> listRoomImages(UUID serviceId, UUID roomId) {
        requireHotelService(serviceId);
        roomRepository.findByIdAndServiceId(roomId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return roomImageRepository.findByRoomId(roomId).stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    private void clearPrimaryImage(UUID roomId) {
        roomImageRepository.clearPrimaryByRoomId(roomId);
    }

    private void recalculateServiceRoomTotals(UUID serviceId) {
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        List<HotelRoom> rooms = roomRepository.findByServiceId(serviceId);
        int total = rooms.stream().mapToInt(r -> r.getQuantityTotal() != null ? r.getQuantityTotal() : 0).sum();
        int available = rooms.stream().mapToInt(r -> r.getQuantityAvailable() != null ? r.getQuantityAvailable() : 0).sum();
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

    private HotelRoomResponse toRoomResponse(HotelRoom room) {
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
                .images(roomImageRepository.findByRoomId(room.getId()).stream()
                        .map(this::toImageResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private HotelRoomImageResponse toImageResponse(HotelRoomImage image) {
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
