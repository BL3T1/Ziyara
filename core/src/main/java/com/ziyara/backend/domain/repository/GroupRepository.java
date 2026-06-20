package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Group;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository {
    List<Group> findAll();

    Optional<Group> findById(UUID id);

    Group save(Group group);

    boolean existsByCode(String code);

    /** True if another group (not {@code excludeId}) already uses {@code code}. */
    boolean existsByCodeAndIdNot(String code, UUID excludeId);

    void deleteById(UUID id);

    Optional<Integer> findMaxCustomGroupCodeSuffix();
}
