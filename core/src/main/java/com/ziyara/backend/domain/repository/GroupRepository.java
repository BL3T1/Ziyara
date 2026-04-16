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
}
