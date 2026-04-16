package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuItemJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuSectionJpaRepository;
import com.ziyara.backend.presentation.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantMenuServiceTest {

    @Mock
    private RestMenuSectionJpaRepository sectionRepository;

    @Mock
    private RestMenuItemJpaRepository itemRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceQueryHandler serviceQueryHandler;

    @InjectMocks
    private RestaurantMenuService restaurantMenuService;

    @Test
    void createSection_WhenNotRestaurant_ShouldThrow() {
        UUID sid = UUID.randomUUID();
        Service svc = new Service();
        svc.setId(sid);
        svc.setType(ServiceType.HOTEL);
        when(serviceQueryHandler.findById(sid))
                .thenReturn(Optional.of(ServiceResponse.builder().id(sid).build()));
        when(serviceRepository.findById(sid)).thenReturn(Optional.of(svc));

        assertThrows(
                BusinessException.class,
                () -> restaurantMenuService.createSection(
                        sid, CreateMenuSectionRequest.builder().title("Starters").build()));
    }

    @Test
    void getMenu_WhenHotel_ShouldReturnEmptySections() {
        UUID sid = UUID.randomUUID();
        Service svc = new Service();
        svc.setType(ServiceType.RESORT);
        when(serviceQueryHandler.findById(sid))
                .thenReturn(Optional.of(ServiceResponse.builder().id(sid).build()));
        when(serviceRepository.findById(sid)).thenReturn(Optional.of(svc));

        RestaurantMenuResponse menu = restaurantMenuService.getMenu(sid);

        assertEquals(sid, menu.getServiceId());
        assertNotNull(menu.getSections());
        assertTrue(menu.getSections().isEmpty());
    }
}
