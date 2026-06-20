package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertContentPageRequest;
import com.ziyara.backend.application.dto.response.ContentPageResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.ContentPage;
import com.ziyara.backend.domain.repository.ContentPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentPageService {

    private final ContentPageRepository contentPageRepository;

    @Transactional(readOnly = true)
    public ContentPageResponse getPublicPage(String slug, String lang) {
        ContentPage page = contentPageRepository
                .findBySlug(normalizeSlug(slug))
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .orElseGet(() -> createFallbackPage(normalizeSlug(slug)));
        Map<String, Object> localized = resolveLocalizedContent(page, lang);
        return ContentPageResponse.builder()
                .slug(page.getSlug())
                .content(localized)
                .published(page.getPublished())
                .updatedAt(page.getUpdatedAt())
                .build();
    }

    @Transactional
    public ContentPageResponse upsert(String slug, UpsertContentPageRequest request) {
        String normalizedSlug = normalizeSlug(slug);
        ContentPage page = contentPageRepository.findBySlug(normalizedSlug)
                .orElseGet(() -> {
                    ContentPage p = new ContentPage();
                    p.setSlug(normalizedSlug);
                    return p;
                });
        page.setContentEn(copyOrEmpty(request.getContentEn()));
        page.setContentAr(copyOrEmpty(request.getContentAr()));
        page.setPublished(request.getPublished() != null ? request.getPublished() : Boolean.TRUE);
        ContentPage saved = contentPageRepository.save(page);
        return ContentPageResponse.builder()
                .slug(saved.getSlug())
                .content(resolveLocalizedContent(saved, "en"))
                .published(saved.getPublished())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private Map<String, Object> resolveLocalizedContent(ContentPage page, String lang) {
        boolean useArabic = (lang != null && lang.toLowerCase().startsWith("ar")) || RequestLocaleHolder.isArabic();
        Map<String, Object> preferred = useArabic ? page.getContentAr() : page.getContentEn();
        Map<String, Object> fallback = useArabic ? page.getContentEn() : page.getContentAr();
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

    private static ContentPage createFallbackPage(String slug) {
        ContentPage p = new ContentPage();
        p.setSlug(slug);
        p.setContentEn(new HashMap<>());
        p.setContentAr(new HashMap<>());
        p.setPublished(true);
        return p;
    }
}
