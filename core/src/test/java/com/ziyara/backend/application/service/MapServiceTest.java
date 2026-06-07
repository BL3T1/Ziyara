package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock ServiceRepository serviceRepository;

    @InjectMocks MapService mapService;

    private static final UUID PROVIDER_ID = UUID.randomUUID();

    @Test
    void getProviderPins_emptyTypes_returnsAllActive() {
        Service s = service(UUID.randomUUID(), PROVIDER_ID);
        when(serviceRepository.findActiveWithCoordinates(List.of())).thenReturn(List.of(s));

        List<ProviderMapPinResponse> pins = mapService.getProviderPins(List.of());

        assertThat(pins).hasSize(1);
    }

    @Test
    void getProviderPins_filteredByType_returnsResults() {
        Service s = service(UUID.randomUUID(), PROVIDER_ID);
        List<String> types = List.of("HOTEL");
        when(serviceRepository.findActiveWithCoordinates(types)).thenReturn(List.of(s));

        List<ProviderMapPinResponse> pins = mapService.getProviderPins(types);

        assertThat(pins).hasSize(1);
    }

    @Test
    void getPortalPins_returnsPinsForProvider() {
        Service s = service(UUID.randomUUID(), PROVIDER_ID);
        when(serviceRepository.findByProviderIdWithCoordinates(PROVIDER_ID)).thenReturn(List.of(s));

        List<ProviderMapPinResponse> pins = mapService.getPortalPins(PROVIDER_ID);

        assertThat(pins).hasSize(1);
        assertThat(pins.get(0).getLatitude()).isEqualTo(10.5);
    }

    @Test
    void toPin_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Service s = service(id, PROVIDER_ID);
        when(serviceRepository.findByProviderIdWithCoordinates(PROVIDER_ID)).thenReturn(List.of(s));

        ProviderMapPinResponse pin = mapService.getPortalPins(PROVIDER_ID).get(0);

        assertThat(pin.getId()).isEqualTo(id);
        assertThat(pin.getLatitude()).isEqualTo(10.5);
        assertThat(pin.getLongitude()).isEqualTo(20.5);
        assertThat(pin.getType()).isEqualTo("HOTEL");
        assertThat(pin.getStatus()).isEqualTo("ACTIVE");
    }

    private Service service(UUID id, UUID providerId) {
        Service s = new Service();
        s.setId(id);
        s.setProviderId(providerId);
        s.setName("Test Hotel");
        s.setType(ServiceType.HOTEL);
        s.setStatus(ServiceStatus.ACTIVE);
        s.setLatitude(BigDecimal.valueOf(10.5));
        s.setLongitude(BigDecimal.valueOf(20.5));
        return s;
    }
}
