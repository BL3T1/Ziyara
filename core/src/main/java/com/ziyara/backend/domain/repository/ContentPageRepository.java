package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ContentPage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPageRepository {

    ContentPage save(ContentPage page);

    Optional<ContentPage> findById(UUID id);

    Optional<ContentPage> findBySlug(String slug);

    List<ContentPage> findAll();

    void deleteById(UUID id);
}
