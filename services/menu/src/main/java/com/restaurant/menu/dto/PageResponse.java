package com.restaurant.menu.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int currentPage,
    int size,
    long totalCount,
    int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int currentPage, int size, long totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        return new PageResponse<>(content, currentPage, size, totalCount, totalPages);
    }
}
