package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.infrastructure.media.LocalMediaStorageService;
import com.ziyara.backend.infrastructure.persistence.entity.RestMenuItemJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.RestMenuSectionJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuItemJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuSectionJpaRepository;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantMenuService {

    private static final int MAX_SECTIONS = 50;
    private static final int MAX_ITEMS_PER_SECTION = 200;

    private final RestMenuSectionJpaRepository sectionRepository;
    private final RestMenuItemJpaRepository itemRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceQueryHandler serviceQueryHandler;
    private final LocalMediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public RestaurantMenuResponse getMenu(UUID serviceId) {
        ensureServiceExists(serviceId);
        if (!isRestaurant(serviceId)) {
            return RestaurantMenuResponse.builder()
                    .serviceId(serviceId)
                    .sections(List.of())
                    .build();
        }
        List<RestaurantMenuSectionResponse> sections = sectionRepository
                .findByServiceIdOrderBySortOrderAscIdAsc(serviceId).stream()
                .map(this::toSectionResponse)
                .collect(Collectors.toList());
        return RestaurantMenuResponse.builder()
                .serviceId(serviceId)
                .sections(sections)
                .build();
    }

    @Transactional
    public RestaurantMenuSectionResponse createSection(UUID serviceId, CreateMenuSectionRequest request) {
        requireRestaurant(serviceId);
        if (sectionRepository.countByServiceId(serviceId) >= MAX_SECTIONS) {
            throw new BusinessException("Maximum " + MAX_SECTIONS + " menu sections per service");
        }
        int sort = request.getSortOrder() != null
                ? request.getSortOrder()
                : (int) Math.min(Integer.MAX_VALUE, sectionRepository.countByServiceId(serviceId));
        RestMenuSectionJpaEntity entity = RestMenuSectionJpaEntity.builder()
                .serviceId(serviceId)
                .title(request.getTitle().trim())
                .sortOrder(sort)
                .build();
        RestMenuSectionJpaEntity saved = sectionRepository.save(entity);
        return toSectionResponse(saved);
    }

    @Transactional
    public RestaurantMenuSectionResponse updateSection(UUID serviceId, UUID sectionId, UpdateMenuSectionRequest request) {
        requireRestaurant(serviceId);
        RestMenuSectionJpaEntity section = loadSectionOwnedByService(sectionId, serviceId);
        if (request.getTitle() != null) {
            String t = request.getTitle().trim();
            if (t.isEmpty()) {
                throw new BusinessException("title cannot be empty");
            }
            section.setTitle(t);
        }
        if (request.getSortOrder() != null) {
            section.setSortOrder(request.getSortOrder());
        }
        return toSectionResponse(sectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(UUID serviceId, UUID sectionId) {
        requireRestaurant(serviceId);
        RestMenuSectionJpaEntity section = loadSectionOwnedByService(sectionId, serviceId);
        sectionRepository.delete(section);
    }

    @Transactional
    public RestaurantMenuItemResponse createItem(UUID serviceId, UUID sectionId, CreateMenuItemRequest request) {
        requireRestaurant(serviceId);
        RestMenuSectionJpaEntity section = loadSectionOwnedByService(sectionId, serviceId);
        if (itemRepository.countBySectionId(sectionId) >= MAX_ITEMS_PER_SECTION) {
            throw new BusinessException("Maximum " + MAX_ITEMS_PER_SECTION + " items per section");
        }
        int sort = request.getSortOrder() != null
                ? request.getSortOrder()
                : (int) Math.min(Integer.MAX_VALUE, itemRepository.countBySectionId(sectionId));
        RestMenuItemJpaEntity entity = RestMenuItemJpaEntity.builder()
                .sectionId(sectionId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .currency(trimOrNull(request.getCurrency()))
                .imageUrl(trimOrNull(request.getImageUrl()))
                .sortOrder(sort)
                .build();
        return toItemResponse(itemRepository.save(entity));
    }

    @Transactional
    public RestaurantMenuItemResponse updateItem(UUID serviceId, UUID itemId, UpdateMenuItemRequest request) {
        requireRestaurant(serviceId);
        RestMenuItemJpaEntity item = loadItemOwnedByService(itemId, serviceId);
        if (request.getName() != null) {
            String n = request.getName().trim();
            if (n.isEmpty()) {
                throw new BusinessException("name cannot be empty");
            }
            item.setName(n);
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            item.setCurrency(trimOrNull(request.getCurrency()));
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(trimOrNull(request.getImageUrl()));
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }
        return toItemResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID serviceId, UUID itemId) {
        requireRestaurant(serviceId);
        RestMenuItemJpaEntity item = loadItemOwnedByService(itemId, serviceId);
        itemRepository.delete(item);
    }

    @Transactional
    public RestaurantMenuItemResponse uploadItemImage(
            UUID serviceId,
            UUID itemId,
            byte[] fileBytes,
            String contentType,
            String originalFilename) {
        requireRestaurant(serviceId);
        RestMenuItemJpaEntity item = loadItemOwnedByService(itemId, serviceId);
        String url = mediaStorageService.storeServiceImage(serviceId, fileBytes, contentType, originalFilename);
        item.setImageUrl(url);
        return toItemResponse(itemRepository.save(item));
    }

    private void ensureServiceExists(UUID serviceId) {
        if (serviceQueryHandler.findById(serviceId).isEmpty()) {
            throw new ResourceNotFoundException("Service not found");
        }
    }

    private boolean isRestaurant(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .map(s -> s.getType() == ServiceType.RESTAURANT)
                .orElse(false);
    }

    private void requireRestaurant(UUID serviceId) {
        ensureServiceExists(serviceId);
        if (!isRestaurant(serviceId)) {
            throw new BusinessException("Menu is only available for RESTAURANT services");
        }
    }

    private RestMenuSectionJpaEntity loadSectionOwnedByService(UUID sectionId, UUID serviceId) {
        RestMenuSectionJpaEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu section not found"));
        if (!serviceId.equals(section.getServiceId())) {
            throw new ResourceNotFoundException("Menu section not found");
        }
        return section;
    }

    private RestMenuItemJpaEntity loadItemOwnedByService(UUID itemId, UUID serviceId) {
        RestMenuItemJpaEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        RestMenuSectionJpaEntity section = sectionRepository.findById(item.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        if (!serviceId.equals(section.getServiceId())) {
            throw new ResourceNotFoundException("Menu item not found");
        }
        return item;
    }

    private RestaurantMenuSectionResponse toSectionResponse(RestMenuSectionJpaEntity section) {
        List<RestaurantMenuItemResponse> items = itemRepository
                .findBySectionIdOrderBySortOrderAscIdAsc(section.getId()).stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        return RestaurantMenuSectionResponse.builder()
                .id(section.getId())
                .serviceId(section.getServiceId())
                .title(section.getTitle())
                .sortOrder(section.getSortOrder() != null ? section.getSortOrder() : 0)
                .items(items)
                .build();
    }

    private RestaurantMenuItemResponse toItemResponse(RestMenuItemJpaEntity item) {
        return RestaurantMenuItemResponse.builder()
                .id(item.getId())
                .sectionId(item.getSectionId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .imageUrl(item.getImageUrl())
                .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                .build();
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
