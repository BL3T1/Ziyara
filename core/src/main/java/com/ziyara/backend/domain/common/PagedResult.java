package com.ziyara.backend.domain.common;

import java.util.List;

/**
 * Domain-native pagination result.
 * Replaces Spring's Page<T> in domain repository ports so the domain layer
 * has zero dependency on any framework.
 */
public record PagedResult<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page < totalPages() - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    public static <T> PagedResult<T> of(List<T> content, long totalElements, PageQuery query) {
        return new PagedResult<>(content, totalElements, query.page(), query.size());
    }

    public static <T> PagedResult<T> empty(PageQuery query) {
        return new PagedResult<>(List.of(), 0, query.page(), query.size());
    }
}
