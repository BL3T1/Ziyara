package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.domain.entity.ServiceImage;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.repository.ServiceImageRepository;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceImageService {

    private static final int MAX_IMAGES_PER_SERVICE = 100;

    private final ServiceImageRepository serviceImageRepository;
    private final ServiceQueryHandler serviceQueryHandler;
    private final MediaStorageService localMediaStorageService;

    @Transactional(readOnly = true)
    public List<ServiceImageResponse> list(UUID serviceId) {
        ensureServiceExists(serviceId);
        return serviceImageRepository.findByServiceIdOrdered(serviceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceImageResponse create(UUID serviceId, CreateServiceImageRequest request) {
        ensureServiceExists(serviceId);
        if (serviceImageRepository.countByServiceId(serviceId) >= MAX_IMAGES_PER_SERVICE) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES_PER_SERVICE + " images per service");
        }
        String url = request.getUrl() != null ? request.getUrl().trim() : "";
        if (url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        ServiceImageCategory category = request.getCategory() != null ? request.getCategory() : ServiceImageCategory.PROPERTY;
        String contextKey = normalizeContextKey(request.getContextKey());

        boolean primary = Boolean.TRUE.equals(request.getPrimary());
        if (primary) {
            clearPrimaryForService(serviceId);
        }

        ServiceImage image = new ServiceImage();
        image.setServiceId(serviceId);
        image.setUrl(url);
        image.setAltText(request.getAltText());
        image.setCategory(category);
        image.setContextKey(contextKey);
        image.setPrimary(primary);
        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (int) Math.min(Integer.MAX_VALUE, serviceImageRepository.countByServiceId(serviceId));
        image.setDisplayOrder(order);

        ServiceImage saved = serviceImageRepository.save(image);
        return toResponse(saved);
    }

    /**
     * Store multipart upload on disk and create an image row (same limits as {@link #create}).
     */
    @Transactional
    public ServiceImageResponse uploadAndCreateImage(
            UUID serviceId,
            byte[] fileBytes,
            String contentType,
            String originalFilename,
            String altText,
            ServiceImageCategory category,
            String contextKey,
            Boolean primary) {
        ensureServiceExists(serviceId);
        if (serviceImageRepository.countByServiceId(serviceId) >= MAX_IMAGES_PER_SERVICE) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES_PER_SERVICE + " images per service");
        }
        String publicUrl = localMediaStorageService.storeServiceImage(
                serviceId, fileBytes, contentType, originalFilename);
        ServiceImageCategory cat = category != null ? category : ServiceImageCategory.PROPERTY;
        CreateServiceImageRequest req = CreateServiceImageRequest.builder()
                .url(publicUrl)
                .altText(altText)
                .category(cat)
                .contextKey(contextKey)
                .primary(primary)
                .build();
        return create(serviceId, req);
    }

    @Transactional
    public ServiceImageResponse update(UUID serviceId, UUID imageId, UpdateServiceImageRequest request) {
        ensureServiceExists(serviceId);
        ServiceImage image = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        if (!serviceId.equals(image.getServiceId())) {
            throw new ResourceNotFoundException("Image not found");
        }
        if (request.getUrl() != null) {
            String url = request.getUrl().trim();
            if (url.isEmpty()) {
                throw new IllegalArgumentException("url cannot be empty");
            }
            image.setUrl(url);
        }
        if (request.getAltText() != null) {
            image.setAltText(request.getAltText());
        }
        if (request.getCategory() != null) {
            image.setCategory(request.getCategory());
        }
        if (request.getContextKey() != null) {
            image.setContextKey(normalizeContextKey(request.getContextKey()));
        }
        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getPrimary() != null) {
            if (Boolean.TRUE.equals(request.getPrimary())) {
                clearPrimaryForServiceExcept(serviceId, imageId);
                image.setPrimary(true);
            } else {
                image.setPrimary(false);
            }
        }
        return toResponse(serviceImageRepository.save(image));
    }

    @Transactional
    public void delete(UUID serviceId, UUID imageId) {
        ensureServiceExists(serviceId);
        ServiceImage image = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        if (!serviceId.equals(image.getServiceId())) {
            throw new ResourceNotFoundException("Image not found");
        }
        serviceImageRepository.deleteById(imageId);
    }

    private void ensureServiceExists(UUID serviceId) {
        if (serviceQueryHandler.findById(serviceId).isEmpty()) {
            throw new ResourceNotFoundException("Service not found");
        }
    }

    private void clearPrimaryForService(UUID serviceId) {
        serviceImageRepository.clearPrimaryByServiceId(serviceId);
    }

    private void clearPrimaryForServiceExcept(UUID serviceId, UUID keepImageId) {
        serviceImageRepository.clearPrimaryByServiceIdExcept(serviceId, keepImageId);
    }

    private static String normalizeContextKey(String key) {
        if (key == null) {
            return null;
        }
        String t = key.trim();
        return t.isEmpty() ? null : t;
    }

    private ServiceImageResponse toResponse(ServiceImage img) {
        return ServiceImageResponse.builder()
                .id(img.getId())
                .serviceId(img.getServiceId())
                .url(img.getUrl())
                .altText(img.getAltText())
                .primary(img.isPrimary())
                .displayOrder(img.getDisplayOrder())
                .category(img.getCategory() != null ? img.getCategory() : ServiceImageCategory.PROPERTY)
                .contextKey(img.getContextKey())
                .build();
    }
}
