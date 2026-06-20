package com.ziyara.backend.domain.repository;

import java.util.List;
import java.util.UUID;

public interface UserPasswordHistoryRepository {

    void save(UUID userId, String passwordHash);

    List<String> findHashesByUserId(UUID userId);

    List<UUID> findIdsByUserIdOldestFirst(UUID userId);

    void deleteById(UUID id);
}
