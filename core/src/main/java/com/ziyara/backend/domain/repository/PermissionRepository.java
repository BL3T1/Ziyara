package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
    List<Permission> findAll();
    List<Permission> findAllUnlocked();
    Optional<Permission> findById(UUID id);
}
