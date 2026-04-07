package com.restaurant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank @Size(min = 4, max = 100) String password,
    @NotBlank String nickname
) {}
