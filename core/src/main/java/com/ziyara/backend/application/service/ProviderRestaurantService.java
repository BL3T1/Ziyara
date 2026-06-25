package com.ziyara.backend.application.service;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.domain.entity.ProviderRestaurant;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.ProviderRestaurantRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderRestaurantService {

    private final ProviderRestaurantRepository restaurantRepository;
    private final ServiceProviderRepository providerRepository;

    @Transactional(readOnly = true)
    public ProviderRestaurant getByProviderId(UUID providerId) {
        return restaurantRepository.findByProviderId(providerId).orElse(null);
    }

    @Transactional
    public ProviderRestaurant create(UUID providerId, String name, String nameAr,
                                     String description, String logoUrl,
                                     Map<String, String> openingHours) {
        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Provider not found"));

        if (restaurantRepository.findByProviderId(providerId).isPresent()) {
            throw new BusinessException("Restaurant already exists for this provider");
        }

        ProviderRestaurant restaurant = new ProviderRestaurant();
        restaurant.setProviderId(providerId);
        restaurant.setName(name);
        restaurant.setNameAr(nameAr);
        restaurant.setDescription(description);
        restaurant.setLogoUrl(logoUrl);
        restaurant.setOpeningHours(openingHours);
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public ProviderRestaurant update(UUID providerId, String name, String nameAr,
                                     String description, String logoUrl,
                                     Map<String, String> openingHours) {
        ProviderRestaurant restaurant = restaurantRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException("Restaurant not found for this provider"));

        if (name != null) restaurant.setName(name);
        if (nameAr != null) restaurant.setNameAr(nameAr);
        if (description != null) restaurant.setDescription(description);
        if (logoUrl != null) restaurant.setLogoUrl(logoUrl);
        if (openingHours != null) restaurant.setOpeningHours(openingHours);
        return restaurantRepository.save(restaurant);
    }
}
