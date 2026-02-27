package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.ServiceStatus;
import com.ziyarah.domain.enums.ServiceType;
import com.ziyarah.infrastructure.persistence.entity.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ServiceJpaRepository
 * Infrastructure layer implementation for Service data access
 */
@Repository
public interface ServiceJpaRepository extends JpaRepository<ServiceJpaEntity, UUID> {
    
    List<ServiceJpaEntity> findByProviderId(UUID providerId);
    
    List<ServiceJpaEntity> findByStatus(ServiceStatus status);
    
    List<ServiceJpaEntity> findByType(ServiceType type);
    
    List<ServiceJpaEntity> findByProviderIdAndStatus(UUID providerId, ServiceStatus status);
    
    List<ServiceJpaEntity> findByNameContaining(String name);
    
    List<ServiceJpaEntity> findByCity(String city);
    
    List<ServiceJpaEntity> findByCountry(String country);
    
    List<ServiceJpaEntity> findByCityAndCountry(String city, String country);
    
    List<ServiceJpaEntity> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    List<ServiceJpaEntity> findByTypeAndCity(ServiceType type, String city);
    
    List<ServiceJpaEntity> findByTypeAndStatus(ServiceType type, ServiceStatus status);
    
    long countByStatus(ServiceStatus status);
    
    long countByProviderId(UUID providerId);
    
    @Query("SELECT s FROM ServiceJpaEntity s WHERE s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    List<ServiceJpaEntity> findAvailableServices();
    
    @Query("SELECT CASE WHEN s.availableRooms >= :rooms THEN true ELSE false END " +
           "FROM ServiceJpaEntity s WHERE s.id = :serviceId")
    boolean hasAvailableRooms(@Param("serviceId") UUID serviceId, @Param("rooms") int rooms);
    
    @Query("SELECT s FROM ServiceJpaEntity s WHERE s.status = 'ACTIVE' " +
           "AND s.deletedAt IS NULL " +
           "AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * " +
           "cos(radians(s.longitude) - radians(:lon)) + sin(radians(:lat)) * " +
           "sin(radians(s.latitude)))) <= :radius")
    List<ServiceJpaEntity> findByLocationNear(@Param("lat") BigDecimal latitude,
                                               @Param("lon") BigDecimal longitude,
                                               @Param("radius") double radiusKm);
}
