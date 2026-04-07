# Phase 1: 인프라 + Auth Service + Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Docker Compose 기반 인프라 세팅, Auth Service(회원가입/로그인/JWT), Gateway Service(라우팅/인증/WebSocket)를 구현하여 인증 파이프라인을 완성한다.

**Architecture:** Gateway가 프론트의 단일 진입점 역할을 하며, JWT 검증 후 하위 서비스로 라우팅한다. Auth Service는 BCrypt 패스워드 해싱, JWT 발급/검증, Redis 기반 Refresh Token 관리를 담당한다.

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security, JJWT, SQLite (JDBC), Redis (Lettuce), Docker Compose

**Spec:** `docs/superpowers/specs/2026-04-07-restaurant-msa-design.md`

---

## File Structure

```
restaurant-msa/
├── docker-compose.yml
├── .env.example
│
├── auth-service/
│   ├── Dockerfile
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/
│       ├── java/com/restaurant/auth/
│       │   ├── AuthApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   └── RedisConfig.java
│       │   ├── controller/
│       │   │   └── AuthController.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   └── TokenService.java
│       │   ├── repository/
│       │   │   └── UserRepository.java
│       │   ├── entity/
│       │   │   └── User.java
│       │   ├── dto/
│       │   │   ├── RegisterRequest.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── LoginResponse.java
│       │   │   ├── RefreshRequest.java
│       │   │   ├── TokenResponse.java
│       │   │   ├── VerifyRequest.java
│       │   │   ├── VerifyResponse.java
│       │   │   └── ErrorResponse.java
│       │   └── exception/
│       │       ├── GlobalExceptionHandler.java
│       │       └── AuthException.java
│       └── resources/
│           ├── application.yml
│           └── schema.sql
│
├── gateway-service/
│   ├── Dockerfile
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/
│       ├── java/com/restaurant/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/
│       │   │   ├── RouteConfig.java
│       │   │   ├── WebSocketConfig.java
│       │   │   └── CorsConfig.java
│       │   ├── filter/
│       │   │   └── JwtAuthFilter.java
│       │   ├── handler/
│       │   │   └── OrderWebSocketHandler.java
│       │   └── controller/
│       │       └── InternalWsController.java
│       └── resources/
│           └── application.yml
```

---

### Task 1: 프로젝트 루트 및 Docker Compose 세팅

**Files:**
- Create: `restaurant-msa/docker-compose.yml`
- Create: `restaurant-msa/.env.example`
- Create: `restaurant-msa/.gitignore`

- [ ] **Step 1: 프로젝트 디렉토리 생성**

```bash
mkdir -p restaurant-msa
cd restaurant-msa
git init
```

- [ ] **Step 2: .gitignore 작성**

```gitignore
# Java
*.class
*.jar
build/
.gradle/

# IDE
.idea/
*.iml
.vscode/

# Env
.env

# Data
*.db
data/
```

- [ ] **Step 3: .env.example 작성**

```env
# Auth Service
JWT_SECRET=your-jwt-secret-key-at-least-32-chars-long
REDIS_HOST=redis
REDIS_PORT=6379

# AI Services (Phase 3)
OPENAI_API_KEY=sk-your-key-here
```

- [ ] **Step 4: docker-compose.yml 작성 (Phase 1 서비스만)**

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - backend-net

  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    volumes:
      - auth_data:/data
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=${JWT_SECRET}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - redis
    networks:
      - backend-net

  gateway-service:
    build: ./gateway-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - AUTH_SERVICE_URL=http://auth-service:8081
      - MENU_SERVICE_URL=http://menu-service:8082
      - ORDER_SERVICE_URL=http://order-service:8083
      - REVIEW_SERVICE_URL=http://review-service:8084
    depends_on:
      - auth-service
    networks:
      - backend-net

networks:
  backend-net:
    driver: bridge

volumes:
  redis_data:
  auth_data:
```

- [ ] **Step 5: 커밋**

```bash
git add .
git commit -m "chore: init project with docker-compose, redis, env config"
```

---

### Task 2: Auth Service - Spring Boot 프로젝트 생성

**Files:**
- Create: `auth-service/build.gradle`
- Create: `auth-service/settings.gradle`
- Create: `auth-service/src/main/java/com/restaurant/auth/AuthApplication.java`
- Create: `auth-service/src/main/resources/application.yml`
- Create: `auth-service/src/main/resources/schema.sql`
- Create: `auth-service/Dockerfile`

- [ ] **Step 1: build.gradle 작성**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.restaurant'
version = '0.0.1'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.xerial:sqlite-jdbc:3.45.1.0'
    implementation 'org.springframework:spring-jdbc'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: settings.gradle 작성**

```groovy
rootProject.name = 'auth-service'
```

- [ ] **Step 3: application.yml 작성**

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:sqlite:/data/auth.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

jwt:
  secret: ${JWT_SECRET:default-dev-secret-key-at-least-32-chars}
  access-expiration-ms: 900000
  refresh-expiration-ms: 604800000

springdoc:
  api-docs:
    path: /api/auth/v3/api-docs
  swagger-ui:
    path: /api/auth/swagger-ui.html
```

- [ ] **Step 4: schema.sql 작성**

```sql
CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT UNIQUE NOT NULL,
    password    TEXT NOT NULL,
    nickname    TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'USER',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Seed admin account (password: admin1234, BCrypt hash)
INSERT OR IGNORE INTO users (email, password, nickname, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '관리자', 'ADMIN');
```

- [ ] **Step 5: AuthApplication.java 작성**

```java
package com.restaurant.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

- [ ] **Step 6: Dockerfile 작성**

```dockerfile
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /data
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 7: 빌드 확인**

```bash
cd auth-service
gradle bootJar --no-daemon
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
cd ..
git add auth-service/
git commit -m "feat(auth): init spring boot project with sqlite, redis, jwt deps"
```

---

### Task 3: Auth Service - Entity, Repository, DTO

**Files:**
- Create: `auth-service/src/main/java/com/restaurant/auth/entity/User.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/repository/UserRepository.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/RegisterRequest.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/LoginRequest.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/LoginResponse.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/RefreshRequest.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/TokenResponse.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/VerifyRequest.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/VerifyResponse.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/dto/ErrorResponse.java`

- [ ] **Step 1: User.java**

```java
package com.restaurant.auth.entity;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    public User() {}

    public User(String email, String password, String nickname, String role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: UserRepository.java**

```java
package com.restaurant.auth.repository;

import com.restaurant.auth.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setRole(rs.getString("role"));
        return user;
    };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email) {
        var list = jdbc.query("SELECT * FROM users WHERE email = ?", rowMapper, email);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(Long id) {
        var list = jdbc.query("SELECT * FROM users WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public User save(User user) {
        jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES (?, ?, ?, ?)",
                user.getEmail(), user.getPassword(), user.getNickname(), user.getRole());
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        user.setId(id);
        return user;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }
}
```

- [ ] **Step 3: DTO 클래스들 작성**

`RegisterRequest.java`:
```java
package com.restaurant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank @Size(min = 4, max = 100) String password,
    @NotBlank String nickname
) {}
```

`LoginRequest.java`:
```java
package com.restaurant.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}
```

`LoginResponse.java`:
```java
package com.restaurant.auth.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserInfo user
) {
    public record UserInfo(Long id, String email, String nickname, String role) {}
}
```

`RefreshRequest.java`:
```java
package com.restaurant.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

`TokenResponse.java`:
```java
package com.restaurant.auth.dto;

public record TokenResponse(String accessToken) {}
```

`VerifyRequest.java`:
```java
package com.restaurant.auth.dto;

public record VerifyRequest(String token) {}
```

`VerifyResponse.java`:
```java
package com.restaurant.auth.dto;

public record VerifyResponse(boolean valid, Long userId, String role) {}
```

`ErrorResponse.java`:
```java
package com.restaurant.auth.dto;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
```

- [ ] **Step 4: 커밋**

```bash
git add auth-service/
git commit -m "feat(auth): add User entity, repository, and DTOs"
```

---

### Task 4: Auth Service - TokenService (JWT + Redis)

**Files:**
- Create: `auth-service/src/main/java/com/restaurant/auth/service/TokenService.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/config/RedisConfig.java`

- [ ] **Step 1: RedisConfig.java**

```java
package com.restaurant.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 2: TokenService.java**

```java
package com.restaurant.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final StringRedisTemplate redis;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs,
            StringRedisTemplate redis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.redis = redis;
    }

    public String generateAccessToken(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        String token = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key)
                .compact();
        redis.opsForValue().set("refresh:" + userId, token, refreshExpirationMs, TimeUnit.MILLISECONDS);
        return token;
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean validateRefreshToken(Long userId, String token) {
        String stored = redis.opsForValue().get("refresh:" + userId);
        return token.equals(stored);
    }

    public void removeRefreshToken(Long userId) {
        redis.delete("refresh:" + userId);
    }

    public boolean isValidToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add auth-service/
git commit -m "feat(auth): add TokenService with JWT generation and Redis refresh token"
```

---

### Task 5: Auth Service - AuthService + AuthController

**Files:**
- Create: `auth-service/src/main/java/com/restaurant/auth/service/AuthService.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/controller/AuthController.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/config/SecurityConfig.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/exception/AuthException.java`
- Create: `auth-service/src/main/java/com/restaurant/auth/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: AuthException.java**

```java
package com.restaurant.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
```

- [ ] **Step 2: GlobalExceptionHandler.java**

```java
package com.restaurant.auth.exception;

import com.restaurant.auth.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ErrorResponse.of(e.getStatus().value(), e.getStatus().getReasonPhrase(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "BAD_REQUEST", message));
    }
}
```

- [ ] **Step 3: AuthService.java**

```java
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

    public LoginResponse.UserInfo register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.nickname(), "USER");
        user = userRepository.save(user);
        return new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenService.generateRefreshToken(user.getId());
        var userInfo = new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getRole());

        return new LoginResponse(accessToken, refreshToken, userInfo);
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = tokenService.parseToken(request.refreshToken());
        Long userId = Long.parseLong(claims.getSubject());

        if (!tokenService.validateRefreshToken(userId, request.refreshToken())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(accessToken);
    }

    public VerifyResponse verify(VerifyRequest request) {
        try {
            Claims claims = tokenService.parseToken(request.token());
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            return new VerifyResponse(true, userId, role);
        } catch (Exception e) {
            return new VerifyResponse(false, null, null);
        }
    }

    public void logout(String token) {
        Claims claims = tokenService.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        tokenService.removeRefreshToken(userId);
    }
}
```

- [ ] **Step 4: SecurityConfig.java**

```java
package com.restaurant.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

- [ ] **Step 5: AuthController.java**

```java
package com.restaurant.auth.controller;

import com.restaurant.auth.dto.*;
import com.restaurant.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse.UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verify(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: Docker 빌드 및 실행 테스트**

```bash
cd ..
docker compose up --build auth-service redis -d
```

```bash
# 회원가입 테스트
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test1234","nickname":"테스트"}'
```
Expected: `201` with `{"id":2,"email":"test@test.com","nickname":"테스트","role":"USER"}`

```bash
# 로그인 테스트
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin","password":"admin1234"}'
```
Expected: `200` with accessToken, refreshToken, user info with role "ADMIN"

```bash
docker compose down
```

- [ ] **Step 7: 커밋**

```bash
git add auth-service/
git commit -m "feat(auth): add AuthService, AuthController with register/login/refresh/verify/logout"
```

---

### Task 6: Gateway Service - Spring Boot 프로젝트 생성

**Files:**
- Create: `gateway-service/build.gradle`
- Create: `gateway-service/settings.gradle`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/GatewayApplication.java`
- Create: `gateway-service/src/main/resources/application.yml`
- Create: `gateway-service/Dockerfile`

- [ ] **Step 1: build.gradle**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.restaurant'
version = '0.0.1'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: settings.gradle**

```groovy
rootProject.name = 'gateway-service'
```

- [ ] **Step 3: application.yml**

```yaml
server:
  port: 8080

services:
  auth-url: ${AUTH_SERVICE_URL:http://localhost:8081}
  menu-url: ${MENU_SERVICE_URL:http://localhost:8082}
  order-url: ${ORDER_SERVICE_URL:http://localhost:8083}
  review-url: ${REVIEW_SERVICE_URL:http://localhost:8084}
```

- [ ] **Step 4: GatewayApplication.java**

```java
package com.restaurant.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 5: Dockerfile**

```dockerfile
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 6: 커밋**

```bash
git add gateway-service/
git commit -m "feat(gateway): init spring boot project"
```

---

### Task 7: Gateway Service - JWT 인증 필터 + 라우팅

**Files:**
- Create: `gateway-service/src/main/java/com/restaurant/gateway/filter/JwtAuthFilter.java`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/config/RouteConfig.java`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/config/CorsConfig.java`

- [ ] **Step 1: CorsConfig.java**

```java
package com.restaurant.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 2: RouteConfig.java (프록시 라우팅)**

```java
package com.restaurant.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RouteConfig {

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${services.menu-url}")
    private String menuUrl;

    @Value("${services.order-url}")
    private String orderUrl;

    @Value("${services.review-url}")
    private String reviewUrl;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    public String getAuthUrl() { return authUrl; }
    public String getMenuUrl() { return menuUrl; }
    public String getOrderUrl() { return orderUrl; }
    public String getReviewUrl() { return reviewUrl; }

    public String resolveServiceUrl(String path) {
        if (path.startsWith("/api/auth")) return authUrl;
        if (path.startsWith("/api/menus")) return menuUrl;
        if (path.startsWith("/api/orders")) return orderUrl;
        if (path.startsWith("/api/reviews")) return reviewUrl;
        return null;
    }
}
```

- [ ] **Step 3: JwtAuthFilter.java**

```java
package com.restaurant.gateway.filter;

import com.restaurant.gateway.config.RouteConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    private static final Set<String> SKIP_AUTH_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh"
    );

    public JwtAuthFilter(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // Skip auth for public endpoints
        if (SKIP_AUTH_PATHS.contains(path) || path.startsWith("/ws/") || path.contains("swagger") || path.contains("api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"토큰이 필요합니다.\"}");
            return;
        }

        String token = authHeader.substring(7);

        // Verify token via Auth Service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{\"token\":\"" + token + "\"}", headers);

            ResponseEntity<Map> verifyResponse = restTemplate.exchange(
                routeConfig.getAuthUrl() + "/api/auth/verify",
                HttpMethod.POST, entity, Map.class
            );

            Map body = verifyResponse.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("valid"))) {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"유효하지 않은 토큰입니다.\"}");
                return;
            }

            // Add user info headers for downstream services
            String userId = String.valueOf(body.get("userId"));
            String role = String.valueOf(body.get("role"));

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equals(name)) return userId;
                    if ("X-User-Role".equals(name)) return role;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.add("X-User-Id");
                    names.add("X-User-Role");
                    return Collections.enumeration(names);
                }
            };

            chain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"토큰 검증 실패\"}");
        }
    }
}
```

- [ ] **Step 4: 커밋**

```bash
git add gateway-service/
git commit -m "feat(gateway): add JWT auth filter and route config"
```

---

### Task 8: Gateway Service - 프록시 컨트롤러 + WebSocket

**Files:**
- Create: `gateway-service/src/main/java/com/restaurant/gateway/controller/ProxyController.java`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/config/WebSocketConfig.java`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/handler/OrderWebSocketHandler.java`
- Create: `gateway-service/src/main/java/com/restaurant/gateway/controller/InternalWsController.java`

- [ ] **Step 1: ProxyController.java**

```java
package com.restaurant.gateway.controller;

import com.restaurant.gateway.config.RouteConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Enumeration;

@RestController
public class ProxyController {

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    public ProxyController(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<String> proxy(HttpServletRequest request, @RequestBody(required = false) String body) throws IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetBase = routeConfig.resolveServiceUrl(path);

        if (targetBase == null) {
            return ResponseEntity.status(404).body("{\"error\":\"Service not found\"}");
        }

        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.set(name, request.getHeader(name));
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl, HttpMethod.valueOf(request.getMethod()), entity, String.class
            );
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
```

- [ ] **Step 2: WebSocketConfig.java**

```java
package com.restaurant.gateway.config;

import com.restaurant.gateway.handler.OrderWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;

    public WebSocketConfig(OrderWebSocketHandler orderWebSocketHandler) {
        this.orderWebSocketHandler = orderWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderWebSocketHandler, "/ws/orders/{userId}", "/ws/admin")
                .setAllowedOriginPatterns("*");
    }
}
```

- [ ] **Step 3: OrderWebSocketHandler.java**

```java
package com.restaurant.gateway.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> adminSessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return;

        String path = uri.getPath();
        if (path.startsWith("/ws/admin")) {
            adminSessions.add(session);
        } else if (path.startsWith("/ws/orders/")) {
            String userId = path.substring("/ws/orders/".length());
            userSessions.put(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        adminSessions.remove(session);
        userSessions.values().remove(session);
    }

    public void notifyUser(String userId, String message) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    public void notifyAdmins(String message) {
        adminSessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException ignored) {}
        });
    }
}
```

- [ ] **Step 4: InternalWsController.java**

```java
package com.restaurant.gateway.controller;

import com.restaurant.gateway.handler.OrderWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/internal/ws")
public class InternalWsController {

    private final OrderWebSocketHandler wsHandler;

    public InternalWsController(OrderWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody Map<String, Object> payload) throws IOException {
        String userId = String.valueOf(payload.get("userId"));
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);

        wsHandler.notifyUser(userId, json);
        wsHandler.notifyAdmins(json);

        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 5: 전체 통합 테스트**

```bash
docker compose up --build -d
```

```bash
# Auth 회원가입 (Gateway 경유)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"test1234","nickname":"유저"}'
```
Expected: `201`

```bash
# Auth 로그인 (Gateway 경유)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin","password":"admin1234"}'
```
Expected: `200` with tokens

```bash
# 인증 필요한 경로에 토큰 없이 접근
curl http://localhost:8080/api/menus
```
Expected: `401` "토큰이 필요합니다."

```bash
# 토큰 있지만 하위 서비스 없을 때 (Menu Service 미구현)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin","password":"admin1234"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/menus
```
Expected: 500 또는 connection refused (Menu Service 아직 없으므로 정상)

```bash
docker compose down
```

- [ ] **Step 6: 커밋**

```bash
git add gateway-service/
git commit -m "feat(gateway): add proxy controller, websocket handler, internal notify endpoint"
```

---

## Phase 1 완료 기준

- [ ] `docker compose up --build` 시 redis, auth-service, gateway-service 3개 컨테이너 정상 기동
- [ ] Gateway(:8080)를 통한 회원가입/로그인 정상 동작
- [ ] JWT 토큰 없이 보호된 경로 접근 시 401
- [ ] JWT 토큰으로 인증 후 X-User-Id, X-User-Role 헤더가 하위 서비스에 전달됨
- [ ] WebSocket `/ws/orders/{userId}`, `/ws/admin` 연결 가능
- [ ] `POST /internal/ws/notify` 호출 시 연결된 WebSocket 클라이언트에 메시지 전달

---

## 다음 Phase

- **Phase 2:** Menu Service + Order Service + Review Service
- **Phase 3:** AI Recommendation + AI Review Writer + AI Operations + AI Validation
- **Phase 4:** Customer Web + Admin Web (Vue3 + Vite + nginx)
