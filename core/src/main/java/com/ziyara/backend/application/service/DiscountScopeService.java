package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.entity.RestMenuItem;
import com.ziyara.backend.domain.entity.RestMenuSection;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.RestMenuItemRepository;
import com.ziyara.backend.domain.repository.RestMenuSectionRepository;
import com.ziyara.backend.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces provider, listing (service), hotel room-type, and restaurant menu scope on discount codes.
 */
@Service
@RequiredArgsConstructor
public class DiscountScopeService {

    private final RestMenuItemRepository menuItemRepository;
    private final RestMenuSectionRepository menuSectionRepository;

    public void assertApplicable(
            DiscountCode dc,
            com.ziyara.backend.domain.entity.Service service,
            UUID roomTypeId,
            List<UUID> menuItemIds,
            List<UUID> menuSectionIds) {
        if (dc == null || service == null) {
            return;
        }
        UUID svcId = service.getId();
        UUID providerId = service.getProviderId();

        if (dc.getProviderId() != null && !dc.getProviderId().equals(providerId)) {
            throw new BusinessException("Discount code does not apply to this provider");
        }
        List<UUID> svcScope = dc.getApplicableServiceIds();
        if (svcScope != null && !svcScope.isEmpty()) {
            if (svcId == null || !svcScope.contains(svcId)) {
                throw new BusinessException("Discount code does not apply to this listing");
            }
        }

        ServiceType type = service.getType();
        if (type == ServiceType.HOTEL || type == ServiceType.RESORT) {
            List<UUID> roomTypes = dc.getApplicableRoomTypeIds();
            if (roomTypes != null && !roomTypes.isEmpty()) {
                if (roomTypeId == null || !roomTypes.contains(roomTypeId)) {
                    throw new BusinessException("Discount code does not apply to this room type");
                }
            }
        }

        if (type == ServiceType.RESTAURANT) {
            assertRestaurantScope(dc, svcId, menuItemIds, menuSectionIds);
        }
    }

    private void assertRestaurantScope(
            DiscountCode dc,
            UUID serviceId,
            List<UUID> orderedItemIds,
            List<UUID> orderedSectionIds) {
        List<UUID> itemScope = dc.getApplicableMenuItemIds();
        List<UUID> secScope = dc.getApplicableMenuSectionIds();
        boolean hasItemScope = itemScope != null && !itemScope.isEmpty();
        boolean hasSecScope = secScope != null && !secScope.isEmpty();
        if (!hasItemScope && !hasSecScope) {
            return;
        }

        List<UUID> items = orderedItemIds != null ? orderedItemIds : List.of();
        Set<UUID> inferredSections = new HashSet<>();
        if (orderedSectionIds != null) {
            for (UUID secId : orderedSectionIds) {
                RestMenuSection sec = menuSectionRepository.findById(secId)
                        .orElseThrow(() -> new BusinessException("Invalid menu section"));
                if (!sec.getServiceId().equals(serviceId)) {
                    throw new BusinessException("Menu section does not belong to this restaurant listing");
                }
                inferredSections.add(secId);
            }
        }
        for (UUID itemId : items) {
            menuItemRepository.findById(itemId).ifPresent(mi -> {
                RestMenuSection section = menuSectionRepository.findById(mi.getSectionId())
                        .orElseThrow(() -> new BusinessException("Invalid menu item for this booking"));
                if (!section.getServiceId().equals(serviceId)) {
                    throw new BusinessException("Menu item does not belong to this restaurant listing");
                }
                inferredSections.add(mi.getSectionId());
            });
        }

        if (items.isEmpty() && inferredSections.isEmpty()) {
            throw new BusinessException("This discount requires selected menu items or sections on the order");
        }

        boolean itemHit = hasItemScope && items.stream().anyMatch(itemScope::contains);
        boolean secHit = hasSecScope && inferredSections.stream().anyMatch(secScope::contains);

        if (hasItemScope && hasSecScope) {
            if (!itemHit && !secHit) {
                throw new BusinessException("Discount code does not apply to the selected menu items or sections");
            }
        } else if (hasItemScope && !itemHit) {
            throw new BusinessException("Discount code does not apply to the selected menu items");
        } else if (hasSecScope && !secHit) {
            throw new BusinessException("Discount code does not apply to the selected menu sections");
        }
    }

    /**
     * Verifies ordered menu items belong to the service (for pricing/booking without full discount object).
     */
    public void verifyMenuItemsBelongToService(UUID serviceId, List<UUID> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return;
        }
        List<RestMenuItem> found = menuItemRepository.findAllById(menuItemIds);
        if (found.size() != menuItemIds.size()) {
            throw new BusinessException("One or more menu items are invalid");
        }
        for (RestMenuItem mi : found) {
            RestMenuSection sec = menuSectionRepository.findById(mi.getSectionId())
                    .orElseThrow(() -> new BusinessException("Invalid menu structure"));
            if (!sec.getServiceId().equals(serviceId)) {
                throw new BusinessException("Menu item does not belong to this restaurant listing");
            }
        }
    }
}
