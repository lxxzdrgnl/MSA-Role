package com.restaurant.menu.dto;

import com.restaurant.menu.entity.Category;

public record CategoryResponse(
    Long id,
    String name,
    Integer sortOrder
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSortOrder()
        );
    }
}
