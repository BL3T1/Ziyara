package com.ziyara.backend.domain.usecase.content;

import com.ziyara.backend.domain.entity.ContentPage;
import com.ziyara.backend.domain.repository.ContentPageRepository;

import java.util.HashMap;
import java.util.Map;

public class UpsertContentPageUseCase {

    private final ContentPageRepository contentPageRepository;

    public UpsertContentPageUseCase(ContentPageRepository contentPageRepository) {
        this.contentPageRepository = contentPageRepository;
    }

    public ContentPage execute(Input input) {
        ContentPage page = contentPageRepository.findBySlug(input.slug())
                .orElseGet(() -> {
                    ContentPage p = new ContentPage();
                    p.setSlug(input.slug());
                    return p;
                });
        page.setContentEn(input.contentEn() != null ? new HashMap<>(input.contentEn()) : new HashMap<>());
        page.setContentAr(input.contentAr() != null ? new HashMap<>(input.contentAr()) : new HashMap<>());
        page.setPublished(input.published() != null ? input.published() : Boolean.TRUE);
        return contentPageRepository.save(page);
    }

    public record Input(
            String slug,
            Map<String, Object> contentEn,
            Map<String, Object> contentAr,
            Boolean published
    ) {}
}
