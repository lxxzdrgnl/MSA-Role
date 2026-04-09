package com.restaurant.auth.controller;

import com.restaurant.auth.dto.*;
import com.restaurant.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestBody VerifyRequest request) {
        VerifyResponse response = authService.verify(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    @GetMapping("/profile")
    public ResponseEntity<LoginResponse.UserInfo> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PatchMapping("/profile/nickname")
    public ResponseEntity<LoginResponse.UserInfo> updateNickname(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        return ResponseEntity.ok(authService.updateNickname(userId, nickname));
    }

    @PostMapping("/nicknames")
    public ResponseEntity<Map<Long, String>> getNicknames(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(authService.getNicknames(userIds));
    }

    @PostMapping("/promote/{userId}")
    public ResponseEntity<Map<String, String>> promote(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.promote(userId, authHeader);
        return ResponseEntity.ok(Map.of("message", "관리자로 승격되었습니다."));
    }
}
