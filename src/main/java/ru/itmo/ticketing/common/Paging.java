package ru.itmo.ticketing.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

public final class Paging {

    public static final int MAX_SIZE = 50;
    public static final String TOTAL_HEADER = "X-Total-Count";

    private Paging() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        return PageRequest.of(page, Math.min(size, MAX_SIZE), sort);
    }

    public static Pageable of(int page, int size) {
        return of(page, size, Sort.by("id"));
    }

    public static <T> ResponseEntity<PageResponse<T>> withTotal(Page<T> page) {
        return ResponseEntity.ok()
                .header(TOTAL_HEADER, String.valueOf(page.getTotalElements()))
                .body(PageResponse.from(page));
    }
}
