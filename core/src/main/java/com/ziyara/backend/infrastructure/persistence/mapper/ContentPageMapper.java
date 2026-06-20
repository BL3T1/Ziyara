package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ContentPage;
import com.ziyara.backend.infrastructure.persistence.entity.ContentPageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContentPageMapper {

    public ContentPage toDomainEntity(ContentPageJpaEntity entity) {
        if (entity == null) return null;
        ContentPage page = new ContentPage();
        page.setId(entity.getId());
        page.setSlug(entity.getSlug());
        page.setContentEn(entity.getContentEn());
        page.setContentAr(entity.getContentAr());
        page.setPublished(entity.getPublished());
        page.setCreatedAt(entity.getCreatedAt());
        page.setUpdatedAt(entity.getUpdatedAt());
        return page;
    }

    public ContentPageJpaEntity toJpaEntity(ContentPage page) {
        if (page == null) return null;
        return ContentPageJpaEntity.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .contentEn(page.getContentEn())
                .contentAr(page.getContentAr())
                .published(page.getPublished())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
