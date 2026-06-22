package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.GroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupJpaRepository extends JpaRepository<GroupJpaEntity, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("SELECT MAX(CAST(SUBSTRING(g.code, 2) AS INTEGER)) FROM GroupJpaEntity g WHERE g.code LIKE 'C%' AND LENGTH(g.code) > 1")
    Optional<Integer> findMaxCustomGroupCodeSuffix();
}
