package com.ziyara.backend.infrastructure.persistence.util;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bridges the domain pagination types (PageQuery / PagedResult) and Spring Data's
 * Pageable / Page. Lives in the infrastructure layer so the domain stays framework-free.
 *
 * Application services use toSpringPage() to expose Spring Page to controllers while
 * domain repositories stay free of Spring types.
 */
public final class PageConverter {

    private PageConverter() {}

    /** Domain → Spring: convert a PageQuery to a Spring Pageable for JPA repositories. */
    public static Pageable toPageable(PageQuery query) {
        if (query.sortBy() != null && !query.sortBy().isBlank()) {
            Sort sort = query.ascending()
                    ? Sort.by(query.sortBy()).ascending()
                    : Sort.by(query.sortBy()).descending();
            return PageRequest.of(query.page(), query.size(), sort);
        }
        return PageRequest.of(query.page(), query.size());
    }

    /** Spring → domain: map JPA Page<J> to domain PagedResult<D> using a mapper function. */
    public static <J, D> PagedResult<D> toPagedResult(Page<J> page, Function<J, D> mapper) {
        List<D> content = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return new PagedResult<>(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    /** Spring → domain: convert Spring Page<T> to domain PagedResult<T> (same type). */
    public static <T> PagedResult<T> toPagedResult(Page<T> page) {
        return new PagedResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }

    /** Domain → Spring: convert domain PagedResult<T> back to a Spring Page (for service/controller boundaries). */
    public static <T> Page<T> toSpringPage(PagedResult<T> result, PageQuery query) {
        return new PageImpl<>(result.content(), toPageable(query), result.totalElements());
    }

    /** Domain → Spring: map PagedResult<D> to Spring Page<R> using a mapper. */
    public static <D, R> Page<R> toSpringPage(PagedResult<D> result, PageQuery query, Function<D, R> mapper) {
        List<R> mapped = result.content().stream().map(mapper).collect(Collectors.toList());
        return new PageImpl<>(mapped, toPageable(query), result.totalElements());
    }
}
