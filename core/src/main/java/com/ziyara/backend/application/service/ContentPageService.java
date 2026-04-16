package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertContentPageRequest;
import com.ziyara.backend.application.dto.response.ContentPageResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.infrastructure.persistence.entity.ContentPageJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.ContentPageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentPageService {

    private final ContentPageJpaRepository contentPageJpaRepository;

    @Transactional(readOnly = true)
    public ContentPageResponse getPublicPage(String slug, String lang) {
        ContentPageJpaEntity entity = contentPageJpaRepository
                .findBySlugAndPublishedTrue(normalizeSlug(slug))
                .orElseGet(() -> createFallbackEntity(normalizeSlug(slug)));
        Map<String, Object> localized = resolveLocalizedContent(entity, lang);
        return ContentPageResponse.builder()
                .slug(entity.getSlug())
                .content(localized)
                .published(entity.getPublished())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Transactional
    public ContentPageResponse upsert(String slug, UpsertContentPageRequest request) {
        String normalizedSlug = normalizeSlug(slug);
        ContentPageJpaEntity entity = contentPageJpaRepository.findBySlug(normalizedSlug)
                .orElseGet(() -> ContentPageJpaEntity.builder().slug(normalizedSlug).build());
        entity.setContentEn(copyOrEmpty(request.getContentEn()));
        entity.setContentAr(copyOrEmpty(request.getContentAr()));
        entity.setPublished(request.getPublished() != null ? request.getPublished() : Boolean.TRUE);
        ContentPageJpaEntity saved = contentPageJpaRepository.save(entity);
        return ContentPageResponse.builder()
                .slug(saved.getSlug())
                .content(resolveLocalizedContent(saved, "en"))
                .published(saved.getPublished())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private Map<String, Object> resolveLocalizedContent(ContentPageJpaEntity entity, String lang) {
        boolean useArabic = (lang != null && lang.toLowerCase().startsWith("ar")) || RequestLocaleHolder.isArabic();
        Map<String, Object> preferred = useArabic ? entity.getContentAr() : entity.getContentEn();
        Map<String, Object> fallback = useArabic ? entity.getContentEn() : entity.getContentAr();
        if (preferred != null && !preferred.isEmpty()) return preferred;
        if (fallback != null && !fallback.isEmpty()) return fallback;
        return new HashMap<>();
    }

    private static String normalizeSlug(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase();
    }

    private static Map<String, Object> copyOrEmpty(Map<String, Object> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }

    private static ContentPageJpaEntity createFallbackEntity(String slug) {
        return ContentPageJpaEntity.builder()
                .slug(slug)
                .contentEn(new HashMap<>())
                .contentAr(new HashMap<>())
                .published(true)
                .build();
    }
}
