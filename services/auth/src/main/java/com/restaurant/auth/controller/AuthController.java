package com.restaurant.auth.controller;

import com.restaurant.auth.dto.*;
import com.restaurant.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "회원가입", description = "새 사용자를 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/register\",\"status\":400,\"code\":\"VALIDATION_FAILED\",\"message\":\"입력값 검증에 실패했습니다.\",\"details\":{\"email\":\"must not be blank\"}}"))),
        @ApiResponse(responseCode = "409", description = "이메일 중복",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/register\",\"status\":409,\"code\":\"DUPLICATE_RESOURCE\",\"message\":\"이미 존재하는 이메일입니다.\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/register\",\"status\":500,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/login\",\"status\":400,\"code\":\"VALIDATION_FAILED\",\"message\":\"입력값 검증에 실패했습니다.\",\"details\":{\"email\":\"must not be blank\"}}"))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/login\",\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/login\",\"status\":500,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/auth/refresh\",\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"유효하지 않은 리프레시 토큰입니다.\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
