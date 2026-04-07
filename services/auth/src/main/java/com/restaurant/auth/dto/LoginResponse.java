package com.restaurant.auth.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserInfo user
) {
    public record UserInfo(Long id, String email, String nickname, String role) {}
}
