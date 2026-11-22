package com.contactapp.contactmanager.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A simple, stable JSON shape for one page of results.
 * (We do not return Spring's Page object directly because its JSON shape is not guaranteed.)
 */
public record PageResponse<T>(
        List<T> content,
        int page,          // current page number, starts at 0
        int size,          // items per page
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}
