package com.restaurant.menu.dto;

import com.restaurant.menu.entity.Menu;
import java.time.LocalDateTime;

public record MenuResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String description,
    Integer price,
    String imageUrl,
    String tags,
    String allergens,
    Integer spicyLevel,
    Integer cookTimeMinutes,
    Boolean isSoldOut,
    Boolean isBest,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static MenuResponse from(Menu menu, String categoryName) {
        return new MenuResponse(
            menu.getId(),
            menu.getCategoryId(),
            categoryName,
            menu.getName(),
            menu.getDescription(),
            menu.getPrice(),
            menu.getImageUrl(),
            menu.getTags(),
            menu.getAllergens(),
            menu.getSpicyLevel(),
            menu.getCookTimeMinutes(),
            menu.getIsSoldOut(),
            menu.getIsBest(),
            menu.getCreatedAt(),
            menu.getUpdatedAt()
        );
    }
}
