package com.restaurant.auth.dto;

public record VerifyResponse(boolean valid, Long userId, String role) {}
