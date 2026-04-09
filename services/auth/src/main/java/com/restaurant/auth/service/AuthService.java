package com.restaurant.auth.service;

import com.restaurant.auth.dto.*;
import com.restaurant.auth.entity.User;
import com.restaurant.auth.exception.AuthException;
import com.restaurant.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                "USER"
        );
        user = userRepository.save(user);

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getRole())
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getRole())
        );
    }

    public TokenResponse refresh(RefreshRequest request) {
        if (!tokenService.isValidToken(request.refreshToken())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        Claims claims = tokenService.parseToken(request.refreshToken());
        Long userId = Long.parseLong(claims.getSubject());

        if (!tokenService.validateRefreshToken(userId, request.refreshToken())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었거나 유효하지 않습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(accessToken);
    }

    public VerifyResponse verify(VerifyRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            return new VerifyResponse(false, null, null);
        }

        if (!tokenService.isValidToken(request.token())) {
            return new VerifyResponse(false, null, null);
        }

        Claims claims = tokenService.parseToken(request.token());
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        return new VerifyResponse(true, userId, role);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7);
        if (!tokenService.isValidToken(token)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Claims claims = tokenService.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        tokenService.removeRefreshToken(userId);
    }

    public LoginResponse.UserInfo updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        userRepository.updateNickname(userId, nickname);
        return new LoginResponse.UserInfo(user.getId(), user.getEmail(), nickname, user.getRole());
    }

    public LoginResponse.UserInfo getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }

    public java.util.Map<Long, String> getNicknames(java.util.List<Long> userIds) {
        return userRepository.findNicknamesByIds(userIds);
    }

    public void promote(Long userId, String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7);
        Claims claims = tokenService.parseToken(token);
        String callerRole = claims.get("role", String.class);

        if (!"ADMIN".equals(callerRole)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "관리자만 승격할 수 있습니다.");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        userRepository.updateRole(userId, "ADMIN");
    }
}
