package com.ziyara.backend.modules.service.api;

import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;

import java.util.UUID;

public interface RestaurantMenuApi {
    RestaurantMenuResponse getMenu(UUID serviceId);
    RestaurantMenuSectionResponse createSection(UUID serviceId, CreateMenuSectionRequest request);
    RestaurantMenuSectionResponse updateSection(UUID serviceId, UUID sectionId, UpdateMenuSectionRequest request);
    void deleteSection(UUID serviceId, UUID sectionId);
    RestaurantMenuItemResponse createItem(UUID serviceId, UUID sectionId, CreateMenuItemRequest request);
    RestaurantMenuItemResponse updateItem(UUID serviceId, UUID itemId, UpdateMenuItemRequest request);
    void deleteItem(UUID serviceId, UUID itemId);
    RestaurantMenuItemResponse uploadItemImage(UUID serviceId, UUID itemId, byte[] fileBytes,
                                                String contentType, String originalFilename);
}
