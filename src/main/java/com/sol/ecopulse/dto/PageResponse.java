package com.sol.ecopulse.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope.
 *
 * <p>Spring's {@code PageImpl} JSON structure is not guaranteed stable across
 * versions, so we expose an explicit, controlled shape instead of serializing a
 * {@link Page} directly.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
