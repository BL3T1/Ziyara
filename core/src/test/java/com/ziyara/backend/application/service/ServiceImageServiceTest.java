package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.domain.entity.ServiceImage;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.repository.ServiceImageRepository;
import com.ziyara.backend.infrastructure.media.LocalMediaStorageService;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceImageServiceTest {

    @Mock
    private ServiceImageRepository serviceImageRepository;

    @Mock
    private ServiceQueryHandler serviceQueryHandler;

    @Mock
    private LocalMediaStorageService localMediaStorageService;

    @InjectMocks
    private ServiceImageService serviceImageService;

    @Test
    void create_WhenServiceMissing_ShouldThrow() {
        UUID sid = UUID.randomUUID();
        when(serviceQueryHandler.findById(sid)).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> serviceImageService.create(
                        sid, CreateServiceImageRequest.builder().url("https://cdn.example.com/a.jpg").build()));
    }

    @Test
    void create_WhenOverLimit_ShouldThrow() {
        UUID sid = UUID.randomUUID();
        when(serviceQueryHandler.findById(sid))
                .thenReturn(Optional.of(ServiceResponse.builder().id(sid).build()));
        when(serviceImageRepository.countByServiceId(sid)).thenReturn(100L);
        assertThrows(
                IllegalArgumentException.class,
                () -> serviceImageService.create(
                        sid, CreateServiceImageRequest.builder().url("https://cdn.example.com/a.jpg").build()));
    }

    @Test
    void create_ShouldTrimUrlAndPersist() {
        UUID sid = UUID.randomUUID();
        when(serviceQueryHandler.findById(sid))
                .thenReturn(Optional.of(ServiceResponse.builder().id(sid).build()));
        when(serviceImageRepository.countByServiceId(sid)).thenReturn(0L);
        when(serviceImageRepository.findByServiceId(sid)).thenReturn(Collections.emptyList());
        when(serviceImageRepository.save(any(ServiceImage.class)))
                .thenAnswer(invocation -> {
                    ServiceImage img = invocation.getArgument(0);
                    if (img.getId() == null) {
                        img.setId(UUID.randomUUID());
                    }
                    return img;
                });

        ServiceImageResponse out = serviceImageService.create(
                sid,
                CreateServiceImageRequest.builder()
                        .url(" https://cdn.example.com/x.png ")
                        .category(ServiceImageCategory.TRIP)
                        .build());

        assertNotNull(out.getId());
        assertEquals("https://cdn.example.com/x.png", out.getUrl());
        assertEquals(ServiceImageCategory.TRIP, out.getCategory());
        verify(serviceImageRepository).save(any(ServiceImage.class));
    }

    @Test
    void uploadAndCreateImage_ShouldStoreBytesAndPersist() {
        UUID sid = UUID.randomUUID();
        when(serviceQueryHandler.findById(sid))
                .thenReturn(Optional.of(ServiceResponse.builder().id(sid).build()));
        when(serviceImageRepository.countByServiceId(sid)).thenReturn(0L);
        when(serviceImageRepository.findByServiceId(sid)).thenReturn(Collections.emptyList());
        when(localMediaStorageService.storeServiceImage(eq(sid), any(), anyString(), anyString()))
                .thenReturn("/api/v1/media/services/" + sid + "/abc.png");
        when(serviceImageRepository.save(any(ServiceImage.class)))
                .thenAnswer(invocation -> {
                    ServiceImage img = invocation.getArgument(0);
                    if (img.getId() == null) {
                        img.setId(UUID.randomUUID());
                    }
                    return img;
                });

        ServiceImageResponse out = serviceImageService.uploadAndCreateImage(
                sid,
                new byte[] {1, 2, 3},
                "image/png",
                "shot.png",
                "caption",
                ServiceImageCategory.ROOM,
                "suite",
                false);

        assertTrue(out.getUrl().contains("/media/services/"));
        verify(localMediaStorageService).storeServiceImage(eq(sid), any(), eq("image/png"), eq("shot.png"));
        verify(serviceImageRepository).save(any(ServiceImage.class));
    }
}
