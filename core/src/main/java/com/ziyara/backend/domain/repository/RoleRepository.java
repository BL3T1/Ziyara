package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(UUID id);
    Optional<Role> findByCode(String code);
    List<Role> findAllOrderByName();

    List<Role> findByGroupIdOrderByName(UUID groupId);

    List<Role> findByGroupIdIsNullOrderByName();

    boolean existsByCode(String code);
    void deleteById(UUID id);
}
