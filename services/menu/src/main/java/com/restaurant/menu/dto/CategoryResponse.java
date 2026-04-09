package com.restaurant.menu.dto;

import com.restaurant.menu.entity.Category;

public record CategoryResponse(
    Long id,
    String name,
    Integer sortOrder,
    Integer menuCount
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSortOrder(),
            null
        );
    }

    public static CategoryResponse from(Category category, int menuCount) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSortOrder(),
            menuCount
        );
    }
}
