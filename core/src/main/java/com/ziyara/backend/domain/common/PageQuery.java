package com.ziyara.backend.domain.common;

/**
 * Domain-native pagination request.
 * Replaces Spring's Pageable in domain repository ports so the domain layer
 * has zero dependency on any framework.
 */
public record PageQuery(int page, int size, String sortBy, boolean ascending) {

    public PageQuery {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0");
        if (size < 1 || size > 200) throw new IllegalArgumentException("Size must be between 1 and 200");
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, null, true);
    }

    public static PageQuery of(int page, int size, String sortBy, boolean ascending) {
        return new PageQuery(page, size, sortBy, ascending);
    }

    public int offset() {
        return page * size;
    }
}
