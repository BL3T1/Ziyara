package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ServiceRepository
 * Interface for service data access - defined in domain layer
 * Implemented by infrastructure layer (Dependency Inversion)
 */
public interface ServiceRepository {
    
    // CRUD Operations
    Service save(Service service);
    Optional<Service> findById(UUID id);
    void deleteById(UUID id);
    void delete(Service service);
    
    // Query Operations
    List<Service> findAll();
    List<Service> findByProviderId(UUID providerId);
    List<Service> findByStatus(ServiceStatus status);
    List<Service> findByType(ServiceType type);
    List<Service> findByProviderIdAndStatus(UUID providerId, ServiceStatus status);
    
    // Search Operations
    List<Service> findByNameContaining(String name);
    List<Service> findByCity(String city);
    List<Service> findByCountry(String country);
    List<Service> findByCityAndCountry(String city, String country);
    
    // Filter Operations
    List<Service> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Service> findByTypeAndCity(ServiceType type, String city);
    List<Service> findByTypeAndStatus(ServiceType type, ServiceStatus status);
    
    // Location-based search
    List<Service> findByLocationNear(BigDecimal latitude, BigDecimal longitude, double radiusKm);
    
    // Availability
    List<Service> findAvailableServices();
    boolean hasAvailableRooms(UUID serviceId, int rooms);
    
    // Statistics
    long count();
    long countByStatus(ServiceStatus status);
    long countByProviderId(UUID providerId);
    
    // Existence checks
    boolean existsById(UUID id);

    List<Service> findActiveWithCoordinates(List<String> types);
    List<Service> findByProviderIdWithCoordinates(UUID providerId);
}
