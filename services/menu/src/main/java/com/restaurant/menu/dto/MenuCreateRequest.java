package com.restaurant.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
    @NotNull(message = "카테고리 ID는 필수입니다")
    Long categoryId,

    @NotBlank(message = "메뉴명은 필수입니다")
    String name,

    String description,

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    Integer price,

    String tags,
    String allergens,
    Integer spicyLevel,
    Integer cookTimeMinutes
) {}
