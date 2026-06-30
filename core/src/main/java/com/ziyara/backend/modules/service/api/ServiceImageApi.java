package com.ziyara.backend.modules.service.api;

import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.domain.enums.ServiceImageCategory;

import java.util.List;
import java.util.UUID;

public interface ServiceImageApi {
    List<ServiceImageResponse> list(UUID serviceId);
    ServiceImageResponse create(UUID serviceId, CreateServiceImageRequest request);
    ServiceImageResponse update(UUID serviceId, UUID imageId, UpdateServiceImageRequest request);
    void delete(UUID serviceId, UUID imageId);
    ServiceImageResponse uploadAndCreateImage(UUID serviceId, byte[] fileBytes, String contentType,
                                               String originalFilename, String altText,
                                               ServiceImageCategory category, String contextKey,
                                               Boolean primary);
}
