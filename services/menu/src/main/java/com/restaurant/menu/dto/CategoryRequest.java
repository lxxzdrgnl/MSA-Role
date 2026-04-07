package com.restaurant.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
    @NotBlank(message = "카테고리명은 필수입니다")
    String name,

    Integer sortOrder
) {}
