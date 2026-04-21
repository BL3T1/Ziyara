package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.KafkaStaffNotificationDeliveredEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KafkaStaffNotificationDeliveredJpaRepository
        extends JpaRepository<KafkaStaffNotificationDeliveredEntity, KafkaStaffNotificationDeliveredEntity.Pk> {

    @Query("select case when count(e) > 0 then true else false end from KafkaStaffNotificationDeliveredEntity e "
            + "where e.id.eventId = :eventId and e.id.userId = :userId")
    boolean existsDelivery(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}
