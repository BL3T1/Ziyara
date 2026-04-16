package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ContentPageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentPageJpaRepository extends JpaRepository<ContentPageJpaEntity, UUID> {

    Optional<ContentPageJpaEntity> findBySlug(String slug);

    Optional<ContentPageJpaEntity> findBySlugAndPublishedTrue(String slug);
}
