package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.enums.ProviderType;
import org.springframework.stereotype.Service;

@Service
public class ProviderTypeConfigService {

    public ProviderFeatureSet getFeaturesFor(ProviderType type) {
        if (type == null) type = ProviderType.HOTEL;
        return switch (type) {
            case HOTEL -> new ProviderFeatureSet(true, true, true, "Room", "Suite");
            case RESORT -> new ProviderFeatureSet(true, true, true, "Room", "Suite");
            case APARTMENT -> new ProviderFeatureSet(false, true, false, "Unit", "Unit");
            case EVENT_SPACE -> new ProviderFeatureSet(true, false, false, "Hall", "Space");
            case TOUR_OPERATOR -> new ProviderFeatureSet(false, false, false, "Slot", "Package");
            case RESTAURANT -> new ProviderFeatureSet(false, false, false, "Table", "Section");
            case TAXI -> new ProviderFeatureSet(false, false, false, "Vehicle", "Vehicle");
            case TRIP -> new ProviderFeatureSet(false, false, false, "Slot", "Package");
        };
    }

    public record ProviderFeatureSet(
            boolean hasRestaurant,
            boolean hasFloors,
            boolean hasBookingConflictResolution,
            String spaceLabel,
            String suiteLabel
    ) {}
}
