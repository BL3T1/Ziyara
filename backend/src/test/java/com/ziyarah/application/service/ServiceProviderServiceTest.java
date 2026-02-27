package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateServiceProviderRequest;
import com.ziyarah.application.dto.response.ServiceProviderResponse;
import com.ziyarah.domain.entity.ServiceProvider;
import com.ziyarah.domain.enums.ProviderStatus;
import com.ziyarah.domain.repository.ServiceProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceProviderServiceTest {

    @Mock
    private ServiceProviderRepository providerRepository;

    @InjectMocks
    private ServiceProviderService providerService;

    @Test
    void createProvider_ShouldSaveAndReturnResponse() {
        CreateServiceProviderRequest request = CreateServiceProviderRequest.builder()
                .name("Test Provider")
                .type("HOTEL")
                .email("test@example.com")
                .build();

        ServiceProvider provider = new ServiceProvider();
        provider.setId(UUID.randomUUID());
        provider.setName("Test Provider");

        when(providerRepository.existsByName(anyString())).thenReturn(false);
        when(providerRepository.save(any(ServiceProvider.class))).thenReturn(provider);

        ServiceProviderResponse response = providerService.createProvider(request);

        assertNotNull(response);
        assertEquals("Test Provider", response.getName());
    }
}
