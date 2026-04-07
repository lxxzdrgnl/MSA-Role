package com.restaurant.menu.dto;

import jakarta.validation.constraints.Min;

public record MenuUpdateRequest(
    Long categoryId,
    String name,
    String description,

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    Integer price,

    String tags,
    String allergens,
    Integer spicyLevel,
    Integer cookTimeMinutes
) {}
