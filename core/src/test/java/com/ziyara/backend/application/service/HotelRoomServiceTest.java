package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.HotelRoom;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.HotelRoomStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.HotelRoomImageRepository;
import com.ziyara.backend.domain.repository.HotelRoomRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelRoomServiceTest {

    @Mock ServiceRepository serviceRepository;
    @Mock HotelRoomRepository roomRepository;
    @Mock HotelRoomImageRepository roomImageRepository;
    @Mock MediaStorageService mediaStorageService;

    HotelRoomService service;

    UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new HotelRoomService(serviceRepository, roomRepository, roomImageRepository, mediaStorageService);
    }

    // ── requireHotelService guard ─────────────────────────────────────────────

    @Test
    void listByService_serviceNotFound_throwsResourceNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByService(serviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void listByService_notHotelType_throwsBusinessException() {
        Service svc = hotelService(ServiceType.RESTAURANT);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));

        assertThatThrownBy(() -> service.listByService(serviceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HOTEL");
    }

    @Test
    void listByService_hotelService_returnsMappedRooms() {
        Service svc = hotelService(ServiceType.HOTEL);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));
        HotelRoom room = room(UUID.randomUUID(), 10, 8);
        when(roomRepository.findByServiceId(serviceId)).thenReturn(List.of(room));
        when(roomImageRepository.findByRoomId(any())).thenReturn(List.of());

        List<HotelRoomResponse> result = service.listByService(serviceId);

        assertThat(result).hasSize(1);
    }

    // ── validateQuantities guard ──────────────────────────────────────────────

    @Test
    void create_availableExceedsTotal_throwsBusinessException() {
        Service svc = hotelService(ServiceType.HOTEL);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));

        CreateHotelRoomRequest request = createRequest(5, 10); // available > total

        assertThatThrownBy(() -> service.create(serviceId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quantities");
    }

    @Test
    void create_nullQuantities_throwsBusinessException() {
        Service svc = hotelService(ServiceType.HOTEL);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));

        CreateHotelRoomRequest request = createRequest(null, null);

        assertThatThrownBy(() -> service.create(serviceId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_validRequest_defaultsStatusToActive() {
        Service svc = hotelService(ServiceType.HOTEL);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));

        HotelRoom saved = room(UUID.randomUUID(), 10, 8);
        when(roomRepository.save(any())).thenReturn(saved);
        // recalculateServiceRoomTotals
        when(roomRepository.findByServiceId(serviceId)).thenReturn(List.of(saved));
        when(serviceRepository.save(any())).thenReturn(svc);
        when(roomImageRepository.findByRoomId(any())).thenReturn(List.of());

        CreateHotelRoomRequest request = createRequest(10, 8);
        request.setRoomType("Standard");
        request.setRoomName("Room A");

        service.create(serviceId, request);

        verify(roomRepository).save(argThat(r -> r.getStatus() == HotelRoomStatus.ACTIVE));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_roomNotFound_throwsResourceNotFound() {
        Service svc = hotelService(ServiceType.HOTEL);
        UUID roomId = UUID.randomUUID();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(svc));
        when(roomRepository.findByIdAndServiceId(roomId, serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(serviceId, roomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Service hotelService(ServiceType type) {
        Service svc = new Service();
        svc.setId(serviceId);
        svc.setType(type);
        return svc;
    }

    private HotelRoom room(UUID id, int total, int available) {
        HotelRoom r = new HotelRoom();
        r.setId(id);
        r.setServiceId(serviceId);
        r.setRoomType("Standard");
        r.setRoomName("Room");
        r.setQuantityTotal(total);
        r.setQuantityAvailable(available);
        r.setStatus(HotelRoomStatus.ACTIVE);
        r.setSortOrder(0);
        return r;
    }

    private CreateHotelRoomRequest createRequest(Integer total, Integer available) {
        CreateHotelRoomRequest req = new CreateHotelRoomRequest();
        req.setRoomType("Standard");
        req.setRoomName("Test Room");
        req.setQuantityTotal(total);
        req.setQuantityAvailable(available);
        req.setBasePrice(BigDecimal.valueOf(100));
        return req;
    }
}
