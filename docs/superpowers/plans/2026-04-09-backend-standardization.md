# Backend Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize error handling, pagination, logging, health checks, and API documentation across all 9 backend services (5 Spring Boot + 4 FastAPI).

**Architecture:** Each service gets its own copy of the common patterns (no shared library) to maintain independent deployability. Spring Boot services use a shared ErrorCode enum pattern + GlobalExceptionHandler. FastAPI services use a common error model + exception handler middleware. All services return identical JSON error format.

**Tech Stack:** Spring Boot 3.2.5, Java 17, FastAPI, Pydantic, SpringDoc OpenAPI 2.4.0

---

## File Structure

### Spring Boot (per service pattern)
```
services/{service}/src/main/java/com/restaurant/{service}/
├── exception/
│   ├── ErrorCode.java          (NEW) - 에러 코드 enum
│   ├── BusinessException.java  (NEW) - 비즈니스 예외 클래스
│   └── GlobalExceptionHandler.java (MODIFY) - 통일된 핸들러
├── dto/
│   ├── ErrorResponse.java      (MODIFY) - 통일된 에러 응답 포맷
│   └── PageResponse.java       (MODIFY) - 통일된 페이지 응답 포맷
├── filter/
│   └── LoggingFilter.java      (NEW) - 요청/응답 로깅 필터
├── controller/
│   └── HealthController.java   (NEW) - 헬스체크 엔드포인트
│   └── *Controller.java        (MODIFY) - Swagger 어노테이션 추가
```

### FastAPI (per service pattern)
```
ai-services/{service}/app/
├── errors.py                   (NEW) - ErrorResponse 모델 + 예외 핸들러
├── middleware.py                (NEW) - 로깅 미들웨어
├── main.py                     (MODIFY) - 에러 핸들러/미들웨어 등록, 헬스체크 강화
├── routers/*.py                (MODIFY) - responses 파라미터 추가
```

### Root
```
services/{service}/.env.example (NEW)
ai-services/{service}/.env.example (NEW)
README.md                       (MODIFY)
CLAUDE.md                       (MODIFY)
```

---

### Task 1: Spring Boot 공통 에러 인프라 — Auth Service

Auth 서비스에 에러 코드 enum, BusinessException, 통일된 ErrorResponse를 먼저 구현한다. 이 패턴을 나머지 서비스에 복사한다.

**Files:**
- Create: `services/auth/src/main/java/com/restaurant/auth/exception/ErrorCode.java`
- Create: `services/auth/src/main/java/com/restaurant/auth/exception/BusinessException.java`
- Modify: `services/auth/src/main/java/com/restaurant/auth/dto/ErrorResponse.java`
- Modify: `services/auth/src/main/java/com/restaurant/auth/exception/GlobalExceptionHandler.java`
- Modify: `services/auth/src/main/java/com/restaurant/auth/exception/AuthException.java`

- [ ] **Step 1: ErrorCode enum 생성**

```java
package com.restaurant.auth.exception;

public enum ErrorCode {
    // 400
    BAD_REQUEST(400, "BAD_REQUEST", "잘못된 요청입니다."),
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "입력값 검증에 실패했습니다."),
    INVALID_QUERY_PARAM(400, "INVALID_QUERY_PARAM", "잘못된 쿼리 파라미터입니다."),
    // 401
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    // 403
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    // 404
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    // 409
    DUPLICATE_RESOURCE(409, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    STATE_CONFLICT(409, "STATE_CONFLICT", "리소스 상태 충돌입니다."),
    // 422
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "처리할 수 없는 요청입니다."),
    // 429
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "요청 한도를 초과했습니다."),
    // 500
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(500, "DATABASE_ERROR", "데이터베이스 오류가 발생했습니다."),
    UNKNOWN_ERROR(500, "UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
```

- [ ] **Step 2: BusinessException 생성**

```java
package com.restaurant.auth.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, String> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Map<String, String> getDetails() { return details; }
}
```

- [ ] **Step 3: ErrorResponse 교체**

기존 `ErrorResponse` record를 새 포맷으로 교체:

```java
package com.restaurant.auth.dto;

import com.restaurant.auth.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    String timestamp,
    String path,
    int status,
    String code,
    String message,
    Map<String, String> details
) {
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            errorCode.getMessage(), null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            message, null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message, Map<String, String> details) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            message, details
        );
    }
}
```

- [ ] **Step 4: GlobalExceptionHandler 교체**

```java
package com.restaurant.auth.exception;

import com.restaurant.auth.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        log.warn("Business error [{}]: {}", ec.getCode(), e.getMessage());
        ErrorResponse body = ErrorResponse.of(ec, request.getRequestURI(), e.getMessage(), e.getDetails());
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e, HttpServletRequest request) {
        ErrorCode ec = mapAuthStatus(e.getStatus().value());
        log.warn("Auth error [{}]: {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        String message = "입력값 검증에 실패했습니다.";
        log.warn("Validation error: {}", details);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), message, details));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest request) {
        log.warn("Missing header: {}", e.getHeaderName());
        return ResponseEntity.status(401)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException e, HttpServletRequest request) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException e, HttpServletRequest request) {
        log.error("Database error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.DATABASE_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }

    private ErrorCode mapAuthStatus(int status) {
        return switch (status) {
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.USER_NOT_FOUND;
            case 409 -> ErrorCode.DUPLICATE_RESOURCE;
            default -> ErrorCode.BAD_REQUEST;
        };
    }
}
```

- [ ] **Step 5: 빌드 확인**

Run: `cd /home/rheon/Desktop/projects/OSS/Week5/MAS-Role/services/auth && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add services/auth/src/main/java/com/restaurant/auth/exception/ErrorCode.java \
      services/auth/src/main/java/com/restaurant/auth/exception/BusinessException.java \
      services/auth/src/main/java/com/restaurant/auth/dto/ErrorResponse.java \
      services/auth/src/main/java/com/restaurant/auth/exception/GlobalExceptionHandler.java
git commit -m "feat(auth): add standardized error response format with 15 error codes"
```

---

### Task 2: Spring Boot 공통 에러 인프라 — Menu, Order, Review, Gateway

나머지 4개 서비스에 동일한 에러 인프라를 적용한다. 각 서비스 패키지에 맞게 package 선언만 변경.

**Files:**
- Create: `services/menu/src/main/java/com/restaurant/menu/exception/ErrorCode.java`
- Create: `services/menu/src/main/java/com/restaurant/menu/exception/BusinessException.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/dto/ErrorResponse.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/exception/GlobalExceptionHandler.java`
- Create: `services/order/src/main/java/com/restaurant/order/exception/ErrorCode.java`
- Create: `services/order/src/main/java/com/restaurant/order/exception/BusinessException.java`
- Modify: `services/order/src/main/java/com/restaurant/order/dto/ErrorResponse.java`
- Modify: `services/order/src/main/java/com/restaurant/order/exception/GlobalExceptionHandler.java`
- Create: `services/review/src/main/java/com/restaurant/review/exception/ErrorCode.java`
- Create: `services/review/src/main/java/com/restaurant/review/exception/BusinessException.java`
- Modify: `services/review/src/main/java/com/restaurant/review/dto/ErrorResponse.java`
- Create: `services/review/src/main/java/com/restaurant/review/exception/GlobalExceptionHandler.java`
- Create: `services/gateway/src/main/java/com/restaurant/gateway/exception/ErrorCode.java`
- Create: `services/gateway/src/main/java/com/restaurant/gateway/dto/ErrorResponse.java`

- [ ] **Step 1: Menu 서비스 에러 인프라**

`ErrorCode.java` — Auth와 동일한 내용, `package com.restaurant.menu.exception;`

`BusinessException.java` — Auth와 동일한 내용, `package com.restaurant.menu.exception;`

`ErrorResponse.java` — Auth와 동일한 내용, `package com.restaurant.menu.dto;`, import를 `com.restaurant.menu.exception.ErrorCode`로 변경

`GlobalExceptionHandler.java`:

```java
package com.restaurant.menu.exception;

import com.restaurant.menu.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        log.warn("Business error [{}]: {}", ec.getCode(), e.getMessage());
        ErrorResponse body = ErrorResponse.of(ec, request.getRequestURI(), e.getMessage(), e.getDetails());
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation error: {}", details);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), "입력값 검증에 실패했습니다.", details));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest request) {
        log.warn("Missing header: {}", e.getHeaderName());
        return ResponseEntity.status(401)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException e, HttpServletRequest request) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("File too large: {}", e.getMessage());
        return ResponseEntity.status(413)
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, request.getRequestURI(), "파일 크기가 제한을 초과했습니다 (최대 20MB)."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException e, HttpServletRequest request) {
        log.error("Database error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.DATABASE_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
```

- [ ] **Step 2: Order 서비스 에러 인프라**

`ErrorCode.java` — `package com.restaurant.order.exception;` (동일 내용)

`BusinessException.java` — `package com.restaurant.order.exception;` (동일 내용)

`ErrorResponse.java`:
```java
package com.restaurant.order.dto;

import com.restaurant.order.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    String timestamp,
    String path,
    int status,
    String code,
    String message,
    Map<String, String> details
) {
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            errorCode.getMessage(), null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            message, null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message, Map<String, String> details) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            message, details
        );
    }
}
```

`GlobalExceptionHandler.java`:

```java
package com.restaurant.order.exception;

import com.restaurant.order.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        log.warn("Business error [{}]: {}", ec.getCode(), e.getMessage());
        ErrorResponse body = ErrorResponse.of(ec, request.getRequestURI(), e.getMessage(), e.getDetails());
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation error: {}", details);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), "입력값 검증에 실패했습니다.", details));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest request) {
        log.warn("Missing header: {}", e.getHeaderName());
        return ResponseEntity.status(401)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("State conflict: {}", e.getMessage());
        return ResponseEntity.status(409)
                .body(ErrorResponse.of(ErrorCode.STATE_CONFLICT, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException e, HttpServletRequest request) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException e, HttpServletRequest request) {
        log.error("Database error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.DATABASE_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
```

Order 컨트롤러에서 인라인 try-catch 제거 (cancelOrder):

```java
// OrderController.java — cancelOrder 메서드 수정
@PatchMapping("/{id}/cancel")
public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long userId) {
    OrderResponse response = orderService.cancelOrder(id, userId);
    return ResponseEntity.ok(response);
}
```

그리고 getAllOrders의 인라인 403 처리도 제거:

```java
@GetMapping("/all")
public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String sort) {
    if (!"ADMIN".equals(role)) {
        throw new SecurityException("관리자 권한이 필요합니다.");
    }
    PageResponse<OrderResponse> response = orderService.getAllOrders(page, size, status, sort);
    return ResponseEntity.ok(response);
}
```

updateStatus도 마찬가지:

```java
@PatchMapping("/{id}/status")
public ResponseEntity<OrderResponse> updateStatus(
        @PathVariable Long id,
        @RequestHeader("X-User-Role") String role,
        @Valid @RequestBody OrderStatusRequest request) {
    if (!"ADMIN".equals(role)) {
        throw new SecurityException("관리자 권한이 필요합니다.");
    }
    OrderResponse response = orderService.updateStatus(id, request.getStatus());
    return ResponseEntity.ok(response);
}
```

- [ ] **Step 3: Review 서비스 에러 인프라**

`ErrorCode.java` — `package com.restaurant.review.exception;` (동일 내용)

`BusinessException.java` — `package com.restaurant.review.exception;` (동일 내용)

`ErrorResponse.java` — record 포맷으로 교체 (`package com.restaurant.review.dto;`, import `com.restaurant.review.exception.ErrorCode`)

`GlobalExceptionHandler.java` — 새로 생성:

```java
package com.restaurant.review.exception;

import com.restaurant.review.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        log.warn("Business error [{}]: {}", ec.getCode(), e.getMessage());
        ErrorResponse body = ErrorResponse.of(ec, request.getRequestURI(), e.getMessage(), e.getDetails());
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation error: {}", details);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), "입력값 검증에 실패했습니다.", details));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest request) {
        log.warn("Missing header: {}", e.getHeaderName());
        return ResponseEntity.status(401)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException e, HttpServletRequest request) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, request.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException e, HttpServletRequest request) {
        log.error("Database error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.DATABASE_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
```

- [ ] **Step 4: Gateway 서비스 에러 모델**

Gateway는 프록시이므로 GlobalExceptionHandler는 불필요하지만, RateLimitFilter와 JwtAuthFilter의 에러 응답을 통일된 포맷으로 변경한다.

`ErrorCode.java` — `package com.restaurant.gateway.exception;` (동일 enum)

`ErrorResponse.java`:
```java
package com.restaurant.gateway.dto;

import com.restaurant.gateway.exception.ErrorCode;
import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    String timestamp,
    String path,
    int status,
    String code,
    String message,
    Map<String, String> details
) {
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            errorCode.getMessage(), null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message) {
        return new ErrorResponse(
            Instant.now().toString(), path,
            errorCode.getStatus(), errorCode.getCode(),
            message, null
        );
    }
}
```

RateLimitFilter — JSON 응답 부분을 `ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS, path)` 사용하도록 수정.

JwtAuthFilter — JSON 응답 부분을 `ErrorResponse.of(ErrorCode.UNAUTHORIZED, path)` 사용하도록 수정.

두 필터 모두 ObjectMapper를 사용하여 ErrorResponse를 직렬화:

```java
// 필터에서의 에러 응답 작성 패턴
private void writeErrorResponse(HttpServletResponse response, HttpServletRequest request, ErrorCode errorCode) throws IOException {
    ErrorResponse errorResponse = ErrorResponse.of(errorCode, request.getRequestURI());
    response.setStatus(errorCode.getStatus());
    response.setContentType("application/json;charset=UTF-8");
    ObjectMapper mapper = new ObjectMapper();
    response.getWriter().write(mapper.writeValueAsString(errorResponse));
}
```

- [ ] **Step 5: 전체 빌드 확인**

Run: `cd /home/rheon/Desktop/projects/OSS/Week5/MAS-Role && for dir in services/auth services/menu services/order services/review services/gateway; do echo "=== $dir ===" && (cd $dir && ./gradlew compileJava); done`
Expected: BUILD SUCCESSFUL for all 5

- [ ] **Step 6: 커밋**

```bash
git add services/*/src/main/java/com/restaurant/*/exception/ErrorCode.java \
      services/*/src/main/java/com/restaurant/*/exception/BusinessException.java \
      services/*/src/main/java/com/restaurant/*/dto/ErrorResponse.java \
      services/*/src/main/java/com/restaurant/*/exception/GlobalExceptionHandler.java \
      services/order/src/main/java/com/restaurant/order/controller/OrderController.java \
      services/gateway/src/main/java/com/restaurant/gateway/dto/ErrorResponse.java \
      services/gateway/src/main/java/com/restaurant/gateway/filter/RateLimitFilter.java \
      services/gateway/src/main/java/com/restaurant/gateway/filter/JwtAuthFilter.java
git commit -m "feat: standardize error response format across all Spring Boot services"
```

---

### Task 3: 페이지네이션 표준화 — Menu, Order, Review

PageResponse를 규격에 맞게 통일하고, sort 파라미터와 검색 필터를 추가한다.

**Files:**
- Modify: `services/menu/src/main/java/com/restaurant/menu/dto/PageResponse.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/controller/MenuController.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/service/MenuService.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/repository/MenuRepository.java`
- Modify: `services/order/src/main/java/com/restaurant/order/dto/PageResponse.java`
- Modify: `services/order/src/main/java/com/restaurant/order/controller/OrderController.java`
- Modify: `services/order/src/main/java/com/restaurant/order/service/OrderService.java`
- Modify: `services/order/src/main/java/com/restaurant/order/repository/OrderRepository.java`
- Modify: `services/review/src/main/java/com/restaurant/review/dto/PageResponse.java`
- Modify: `services/review/src/main/java/com/restaurant/review/controller/ReviewController.java`
- Modify: `services/review/src/main/java/com/restaurant/review/service/ReviewService.java`
- Modify: `services/review/src/main/java/com/restaurant/review/repository/ReviewRepository.java`

- [ ] **Step 1: Menu PageResponse 통일**

모든 3개 서비스의 PageResponse를 동일한 포맷으로 변경.

Menu PageResponse:
```java
package com.restaurant.menu.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String sort
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements, String sort) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(content, page, size, totalElements, totalPages, sort);
    }
}
```

Order/Review PageResponse — 동일 구조로 교체 (각각의 package).

- [ ] **Step 2: Menu 서비스 — sort, 필터 파라미터 추가**

MenuController 수정:
```java
@GetMapping
public ResponseEntity<PageResponse<MenuResponse>> getMenus(
        @RequestParam(required = false) Long category,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,DESC") String sort) {
    if (size > 100) size = 100;
    return ResponseEntity.ok(menuService.getMenus(category, keyword, page, size, sort));
}
```

MenuService 수정 — sort 파라미터 파싱하여 SQL ORDER BY에 반영:

```java
public PageResponse<MenuResponse> getMenus(Long categoryId, String keyword, int page, int size, String sort) {
    int offset = page * size;
    String orderBy = parseSortParam(sort);
    List<Menu> menus = menuRepository.search(categoryId, keyword, offset, size, orderBy);
    long totalCount = menuRepository.count(categoryId, keyword);
    List<MenuResponse> content = menus.stream()
        .map(m -> MenuResponse.from(m, getCategoryName(m.getCategoryId())))
        .toList();
    return PageResponse.of(content, page, size, totalCount, sort);
}

private String parseSortParam(String sort) {
    if (sort == null || sort.isBlank()) return "id DESC";
    String[] parts = sort.split(",");
    String field = parts[0];
    String direction = parts.length > 1 ? parts[1].toUpperCase() : "DESC";
    // 허용된 필드만
    String column = switch (field) {
        case "name" -> "name";
        case "price" -> "price";
        case "createdAt" -> "created_at";
        default -> "created_at";
    };
    if (!"ASC".equals(direction) && !"DESC".equals(direction)) direction = "DESC";
    return column + " " + direction;
}
```

MenuRepository.search — orderBy 파라미터 추가:
```java
public List<Menu> search(Long categoryId, String keyword, int offset, int limit, String orderBy) {
    StringBuilder sql = new StringBuilder("SELECT * FROM menus WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (categoryId != null) {
        sql.append(" AND category_id = ?");
        params.add(categoryId);
    }
    if (keyword != null && !keyword.isBlank()) {
        sql.append(" AND (name LIKE ? OR description LIKE ? OR tags LIKE ?)");
        String like = "%" + keyword + "%";
        params.add(like); params.add(like); params.add(like);
    }
    sql.append(" ORDER BY ").append(orderBy).append(" LIMIT ? OFFSET ?");
    params.add(limit); params.add(offset);
    return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
}
```

**주의:** page 기본값을 0으로 변경 (0-indexed). 기존 getMenus는 page=1이 첫 페이지였으므로, offset 계산을 `page * size`로 변경.

- [ ] **Step 3: Order 서비스 — sort, dateFrom/dateTo 필터 추가**

OrderController 수정:
```java
@GetMapping
public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,DESC") String sort) {
    if (size > 100) size = 100;
    return ResponseEntity.ok(orderService.getMyOrders(userId, page, size, sort));
}

@GetMapping("/all")
public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(defaultValue = "createdAt,DESC") String sort) {
    if (!"ADMIN".equals(role)) {
        throw new SecurityException("관리자 권한이 필요합니다.");
    }
    if (size > 100) size = 100;
    return ResponseEntity.ok(orderService.getAllOrders(page, size, status, dateFrom, dateTo, sort));
}
```

OrderService — sort, dateFrom/dateTo 처리 추가. parseSortParam과 유사한 패턴:

```java
private String parseSortParam(String sort) {
    if (sort == null || sort.isBlank()) return "o.created_at DESC";
    String[] parts = sort.split(",");
    String field = parts[0];
    String direction = parts.length > 1 ? parts[1].toUpperCase() : "DESC";
    String column = switch (field) {
        case "totalPrice" -> "o.total_price";
        case "status" -> "o.status";
        case "createdAt" -> "o.created_at";
        default -> "o.created_at";
    };
    if (!"ASC".equals(direction) && !"DESC".equals(direction)) direction = "DESC";
    return column + " " + direction;
}
```

OrderRepository — dateFrom/dateTo, orderBy 파라미터 추가:

```java
public List<Order> findAll(int offset, int size, String status, String dateFrom, String dateTo, String orderBy) {
    StringBuilder sql = new StringBuilder("SELECT * FROM orders o WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
        sql.append(" AND o.status = ?");
        params.add(status);
    }
    if (dateFrom != null && !dateFrom.isBlank()) {
        sql.append(" AND o.created_at >= ?");
        params.add(dateFrom + " 00:00:00");
    }
    if (dateTo != null && !dateTo.isBlank()) {
        sql.append(" AND o.created_at <= ?");
        params.add(dateTo + " 23:59:59");
    }
    sql.append(" ORDER BY ").append(orderBy).append(" LIMIT ? OFFSET ?");
    params.add(size); params.add(offset);
    return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
}
```

page를 0-indexed로 변경: offset = `page * size`.

- [ ] **Step 4: Review 서비스 — sort, rating 필터 추가**

ReviewController 수정:
```java
@GetMapping
public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
        @RequestParam(required = false) Long menuId,
        @RequestParam(required = false) Integer rating,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,DESC") String sort) {
    if (size > 100) size = 100;
    return ResponseEntity.ok(reviewService.getReviews(menuId, rating, page, size, sort));
}
```

ReviewService — rating 필터, sort 추가. ReviewRepository에 해당 쿼리 파라미터 추가.

page를 0-indexed로 변경: offset = `page * size`.

- [ ] **Step 5: 빌드 확인**

Run: `cd /home/rheon/Desktop/projects/OSS/Week5/MAS-Role && for dir in services/menu services/order services/review; do echo "=== $dir ===" && (cd $dir && ./gradlew compileJava); done`
Expected: BUILD SUCCESSFUL for all 3

- [ ] **Step 6: 커밋**

```bash
git add services/menu/src services/order/src services/review/src
git commit -m "feat: standardize pagination with sort, search filters across menu/order/review"
```

---

### Task 4: 요청/응답 로깅 필터 — Spring Boot

모든 5개 Spring Boot 서비스에 LoggingFilter를 추가한다.

**Files:**
- Create: `services/auth/src/main/java/com/restaurant/auth/filter/LoggingFilter.java`
- Create: `services/menu/src/main/java/com/restaurant/menu/filter/LoggingFilter.java`
- Create: `services/order/src/main/java/com/restaurant/order/filter/LoggingFilter.java`
- Create: `services/review/src/main/java/com/restaurant/review/filter/LoggingFilter.java`
- Create: `services/gateway/src/main/java/com/restaurant/gateway/filter/LoggingFilter.java`

- [ ] **Step 1: LoggingFilter 생성 (Auth 서비스 기준)**

```java
package com.restaurant.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} {} {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
        }
    }
}
```

- [ ] **Step 2: 나머지 4개 서비스에 동일 파일 생성**

패키지명만 변경하여 동일한 LoggingFilter를 생성:
- `com.restaurant.menu.filter.LoggingFilter`
- `com.restaurant.order.filter.LoggingFilter`
- `com.restaurant.review.filter.LoggingFilter`
- `com.restaurant.gateway.filter.LoggingFilter` (단, gateway는 `@Order(0)` — RateLimitFilter보다 먼저 기록)

- [ ] **Step 3: 빌드 확인**

Run: `cd /home/rheon/Desktop/projects/OSS/Week5/MAS-Role && for dir in services/auth services/menu services/order services/review services/gateway; do echo "=== $dir ===" && (cd $dir && ./gradlew compileJava); done`

- [ ] **Step 4: 커밋**

```bash
git add services/*/src/main/java/com/restaurant/*/filter/LoggingFilter.java
git commit -m "feat: add request/response logging filter to all Spring Boot services"
```

---

### Task 5: FastAPI 에러 핸들링 + 로깅 미들웨어

4개 FastAPI 서비스에 통일된 에러 응답 모델, 글로벌 예외 핸들러, 로깅 미들웨어를 추가한다.

**Files:**
- Create: `ai-services/recommendation/app/errors.py`
- Create: `ai-services/recommendation/app/middleware.py`
- Modify: `ai-services/recommendation/app/main.py`
- Create: `ai-services/review-writer/app/errors.py`
- Create: `ai-services/review-writer/app/middleware.py`
- Modify: `ai-services/review-writer/app/main.py`
- Create: `ai-services/operations/app/errors.py`
- Create: `ai-services/operations/app/middleware.py`
- Modify: `ai-services/operations/app/main.py`
- Create: `ai-services/validation/app/errors.py`
- Create: `ai-services/validation/app/middleware.py`
- Modify: `ai-services/validation/app/main.py`

- [ ] **Step 1: errors.py 생성 (공통 패턴)**

```python
from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.exceptions import HTTPException as StarletteHTTPException


class ErrorResponse(BaseModel):
    timestamp: str
    path: str
    status: int
    code: str
    message: str
    details: Optional[dict] = None


class BusinessError(Exception):
    def __init__(self, status: int, code: str, message: str, details: dict | None = None):
        self.status = status
        self.code = code
        self.message = message
        self.details = details


def register_error_handlers(app: FastAPI):
    @app.exception_handler(BusinessError)
    async def business_error_handler(request: Request, exc: BusinessError):
        return JSONResponse(
            status_code=exc.status,
            content=ErrorResponse(
                timestamp=datetime.now(timezone.utc).isoformat(),
                path=str(request.url.path),
                status=exc.status,
                code=exc.code,
                message=exc.message,
                details=exc.details,
            ).model_dump(),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError):
        details = {}
        for error in exc.errors():
            field = ".".join(str(loc) for loc in error["loc"] if loc != "body")
            details[field] = error["msg"]
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                timestamp=datetime.now(timezone.utc).isoformat(),
                path=str(request.url.path),
                status=400,
                code="VALIDATION_FAILED",
                message="입력값 검증에 실패했습니다.",
                details=details,
            ).model_dump(),
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_error_handler(request: Request, exc: StarletteHTTPException):
        code_map = {
            400: "BAD_REQUEST",
            401: "UNAUTHORIZED",
            403: "FORBIDDEN",
            404: "RESOURCE_NOT_FOUND",
            429: "TOO_MANY_REQUESTS",
            500: "INTERNAL_SERVER_ERROR",
        }
        return JSONResponse(
            status_code=exc.status_code,
            content=ErrorResponse(
                timestamp=datetime.now(timezone.utc).isoformat(),
                path=str(request.url.path),
                status=exc.status_code,
                code=code_map.get(exc.status_code, "UNKNOWN_ERROR"),
                message=str(exc.detail),
            ).model_dump(),
        )

    @app.exception_handler(Exception)
    async def general_error_handler(request: Request, exc: Exception):
        import logging
        logging.getLogger(__name__).error("Unexpected error: %s", exc, exc_info=True)
        return JSONResponse(
            status_code=500,
            content=ErrorResponse(
                timestamp=datetime.now(timezone.utc).isoformat(),
                path=str(request.url.path),
                status=500,
                code="INTERNAL_SERVER_ERROR",
                message="서버 내부 오류가 발생했습니다.",
            ).model_dump(),
        )
```

이 파일을 4개 서비스 모두에 동일하게 생성.

- [ ] **Step 2: middleware.py 생성 (공통 패턴)**

```python
import logging
import time

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

logger = logging.getLogger(__name__)


class LoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        start_time = time.time()
        response = await call_next(request)
        duration_ms = int((time.time() - start_time) * 1000)
        logger.info(
            "%s %s %s %dms",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
        )
        return response
```

이 파일을 4개 서비스 모두에 동일하게 생성.

- [ ] **Step 3: main.py 수정 — Recommendation 서비스**

main.py에 에러 핸들러와 미들웨어를 등록:

```python
# 기존 import 이후에 추가
from app.errors import register_error_handlers
from app.middleware import LoggingMiddleware

# FastAPI 인스턴스 생성 직후에 추가
register_error_handlers(app)
app.add_middleware(LoggingMiddleware)
```

- [ ] **Step 4: main.py 수정 — Review Writer, Operations, Validation**

3개 서비스 모두 동일한 패턴으로 main.py에 등록.

- [ ] **Step 5: 커밋**

```bash
git add ai-services/*/app/errors.py ai-services/*/app/middleware.py ai-services/*/app/main.py
git commit -m "feat: add standardized error handling and logging middleware to all FastAPI services"
```

---

### Task 6: 헬스체크 엔드포인트

Spring Boot 서비스에 `/health` 엔드포인트를 추가하고, FastAPI 서비스의 기존 `/health`에 버전/빌드시간을 추가한다.

**Files:**
- Create: `services/auth/src/main/java/com/restaurant/auth/controller/HealthController.java`
- Create: `services/menu/src/main/java/com/restaurant/menu/controller/HealthController.java`
- Create: `services/order/src/main/java/com/restaurant/order/controller/HealthController.java`
- Create: `services/review/src/main/java/com/restaurant/review/controller/HealthController.java`
- Create: `services/gateway/src/main/java/com/restaurant/gateway/controller/HealthController.java`
- Modify: `ai-services/recommendation/app/main.py`
- Modify: `ai-services/review-writer/app/main.py`
- Modify: `ai-services/operations/app/main.py`
- Modify: `ai-services/validation/app/main.py`

- [ ] **Step 1: Spring Boot HealthController (공통 패턴)**

Auth 서비스 기준:
```java
package com.restaurant.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private static final String VERSION = "1.0.0";
    private static final String BUILD_TIME = Instant.now().toString();

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", VERSION,
                "buildTime", BUILD_TIME
        ));
    }
}
```

나머지 4개 서비스에도 동일한 패턴 (패키지명만 변경).

- [ ] **Step 2: FastAPI 헬스체크 강화**

각 main.py의 health_check 함수 수정:

```python
import time

_start_time = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

@app.get("/health")
async def health_check():
    return {"status": "UP", "version": "1.0.0", "buildTime": _start_time}
```

- [ ] **Step 3: 커밋**

```bash
git add services/*/src/main/java/com/restaurant/*/controller/HealthController.java \
      ai-services/*/app/main.py
git commit -m "feat: add health check endpoints with version and build time to all services"
```

---

### Task 7: .env.example 파일

각 서비스에 환경변수 예시 파일을 추가한다.

**Files:**
- Create: `services/auth/.env.example`
- Create: `services/gateway/.env.example`
- Create: `services/menu/.env.example`
- Create: `services/order/.env.example`
- Create: `services/review/.env.example`
- Create: `ai-services/recommendation/.env.example`
- Create: `ai-services/review-writer/.env.example`
- Create: `ai-services/operations/.env.example`
- Create: `ai-services/validation/.env.example`

- [ ] **Step 1: Spring Boot .env.example 파일 생성**

`services/auth/.env.example`:
```
# Auth Service Environment Variables
JWT_SECRET=your-jwt-secret-key-at-least-32-characters
REDIS_HOST=localhost
REDIS_PORT=6379
```

`services/gateway/.env.example`:
```
# Gateway Service Environment Variables
AUTH_SERVICE_URL=http://localhost:8081
MENU_SERVICE_URL=http://localhost:8082
ORDER_SERVICE_URL=http://localhost:8083
REVIEW_SERVICE_URL=http://localhost:8084
AI_RECOMMENDATION_URL=http://localhost:8001
AI_OPERATIONS_URL=http://localhost:8003
```

`services/menu/.env.example`:
```
# Menu Service Environment Variables
AI_RECOMMENDATION_URL=http://localhost:8001
```

`services/order/.env.example`:
```
# Order Service Environment Variables
MENU_SERVICE_URL=http://localhost:8082
GATEWAY_URL=http://localhost:8080
```

`services/review/.env.example`:
```
# Review Service Environment Variables
AUTH_SERVICE_URL=http://localhost:8081
AI_REVIEW_WRITER_URL=http://localhost:8002
AI_RECOMMENDATION_URL=http://localhost:8001
AI_VALIDATION_URL=http://localhost:8004
REDIS_HOST=localhost
REDIS_PORT=6379
```

- [ ] **Step 2: FastAPI .env.example 파일 생성**

`ai-services/recommendation/.env.example`:
```
# AI Recommendation Service Environment Variables
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4o-mini
MENU_SERVICE_URL=http://localhost:8082
CHROMA_PERSIST_PATH=/data/chroma
EMBEDDING_MODEL=jhgan/ko-sroberta-multitask
```

`ai-services/review-writer/.env.example`:
```
# AI Review Writer Service Environment Variables
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4o-mini
```

`ai-services/operations/.env.example`:
```
# AI Operations Service Environment Variables
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4o-mini
ORDER_SERVICE_URL=http://localhost:8083
MENU_SERVICE_URL=http://localhost:8082
CONGESTION_LOW=5
CONGESTION_HIGH=15
```

`ai-services/validation/.env.example`:
```
# AI Validation Service Environment Variables
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4o-mini
```

- [ ] **Step 3: 커밋**

```bash
git add services/*/.env.example ai-services/*/.env.example
git commit -m "feat: add .env.example files for all services"
```

---

### Task 8: Swagger 에러 응답 문서화 — Spring Boot

각 컨트롤러에 `@ApiResponse` 어노테이션으로 에러 응답 예시를 추가한다.

**Files:**
- Modify: `services/auth/src/main/java/com/restaurant/auth/controller/AuthController.java`
- Modify: `services/menu/src/main/java/com/restaurant/menu/controller/MenuController.java`
- Modify: `services/order/src/main/java/com/restaurant/order/controller/OrderController.java`
- Modify: `services/review/src/main/java/com/restaurant/review/controller/ReviewController.java`

- [ ] **Step 1: Auth Controller — Swagger 어노테이션 추가**

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

각 엔드포인트에 적용 (예시 — login):

```java
@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {"timestamp":"2025-03-05T12:00:00Z","path":"/api/auth/login","status":400,"code":"VALIDATION_FAILED","message":"입력값 검증에 실패했습니다.","details":{"email":"must not be blank"}}
                """))),
    @ApiResponse(responseCode = "401", description = "인증 실패",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {"timestamp":"2025-03-05T12:00:00Z","path":"/api/auth/login","status":401,"code":"UNAUTHORIZED","message":"이메일 또는 비밀번호가 올바르지 않습니다."}
                """))),
    @ApiResponse(responseCode = "500", description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {"timestamp":"2025-03-05T12:00:00Z","path":"/api/auth/login","status":500,"code":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다."}
                """)))
})
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) { ... }
```

Register:
```java
@Operation(summary = "회원가입", description = "새 사용자를 등록합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "회원가입 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "409", description = "이메일 중복",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {"timestamp":"2025-03-05T12:00:00Z","path":"/api/auth/register","status":409,"code":"DUPLICATE_RESOURCE","message":"이미 존재하는 이메일입니다."}
                """))),
    @ApiResponse(responseCode = "500", description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
```

나머지 엔드포인트도 유사하게 적용. 각 엔드포인트에 해당하는 에러 코드만 포함 (400/401/403/404/422/500 중 관련된 것만).

- [ ] **Step 2: Menu Controller — Swagger 어노테이션 추가**

주요 엔드포인트에 적용:

GET /api/menus: 200, 400 (INVALID_QUERY_PARAM)
GET /api/menus/{id}: 200, 404 (RESOURCE_NOT_FOUND)
POST /api/menus: 201, 400 (VALIDATION_FAILED), 401 (UNAUTHORIZED), 403 (FORBIDDEN), 500
PUT /api/menus/{id}: 200, 400, 401, 403, 404, 500
DELETE /api/menus/{id}: 204, 401, 403, 404, 500

```java
@Operation(summary = "메뉴 목록 조회", description = "페이지네이션, 카테고리/키워드 필터, 정렬을 지원합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 쿼리 파라미터",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {"timestamp":"2025-03-05T12:00:00Z","path":"/api/menus","status":400,"code":"INVALID_QUERY_PARAM","message":"잘못된 쿼리 파라미터입니다."}
                """)))
})
@GetMapping
public ResponseEntity<PageResponse<MenuResponse>> getMenus(...) { ... }
```

- [ ] **Step 3: Order Controller — Swagger 어노테이션 추가**

주요 엔드포인트 — 특히 상태 변경 엔드포인트에 STATE_CONFLICT 포함:

PATCH /api/orders/{id}/cancel: 200, 403 (FORBIDDEN), 409 (STATE_CONFLICT), 404
PATCH /api/orders/{id}/status: 200, 400 (VALIDATION_FAILED), 403, 404, 500

- [ ] **Step 4: Review Controller — Swagger 어노테이션 추가**

POST /api/reviews: 201, 400 (VALIDATION_FAILED), 401, 422 (UNPROCESSABLE_ENTITY), 500
GET /api/reviews: 200
GET /api/reviews/{id}: 200, 404
DELETE /api/reviews/{id}: 204, 401, 403, 404

- [ ] **Step 5: 빌드 확인**

Run: `cd /home/rheon/Desktop/projects/OSS/Week5/MAS-Role && for dir in services/auth services/menu services/order services/review; do echo "=== $dir ===" && (cd $dir && ./gradlew compileJava); done`

- [ ] **Step 6: 커밋**

```bash
git add services/auth/src services/menu/src services/order/src services/review/src
git commit -m "feat: add Swagger error response examples to all Spring Boot controllers"
```

---

### Task 9: FastAPI Swagger 에러 응답 문서화

4개 FastAPI 서비스의 라우터에 `responses` 파라미터로 에러 응답 예시를 추가한다.

**Files:**
- Modify: `ai-services/recommendation/app/routers/recommendations.py`
- Modify: `ai-services/recommendation/app/routers/embeddings.py`
- Modify: `ai-services/review-writer/app/routers/reviews.py`
- Modify: `ai-services/operations/app/routers/operations.py`
- Modify: `ai-services/validation/app/routers/validation.py`

- [ ] **Step 1: 공통 에러 응답 정의**

각 서비스의 errors.py에 추가:

```python
# 공통 Swagger 에러 응답 정의
ERROR_RESPONSES = {
    400: {
        "description": "입력값 검증 실패",
        "content": {
            "application/json": {
                "example": {
                    "timestamp": "2025-03-05T12:00:00Z",
                    "path": "/example",
                    "status": 400,
                    "code": "VALIDATION_FAILED",
                    "message": "입력값 검증에 실패했습니다.",
                    "details": {"field": "error message"},
                }
            }
        },
    },
    422: {
        "description": "처리할 수 없는 요청",
        "content": {
            "application/json": {
                "example": {
                    "timestamp": "2025-03-05T12:00:00Z",
                    "path": "/example",
                    "status": 422,
                    "code": "UNPROCESSABLE_ENTITY",
                    "message": "처리할 수 없는 요청입니다.",
                }
            }
        },
    },
    500: {
        "description": "서버 내부 오류",
        "content": {
            "application/json": {
                "example": {
                    "timestamp": "2025-03-05T12:00:00Z",
                    "path": "/example",
                    "status": 500,
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                }
            }
        },
    },
}
```

- [ ] **Step 2: Recommendation 라우터 수정**

```python
from app.errors import ERROR_RESPONSES

@router.post("/chat", response_model=RecommendationResponse, responses=ERROR_RESPONSES)
async def chat_recommend(request: ChatRequest):
    ...

@router.post("/keyword", response_model=RecommendationResponse, responses=ERROR_RESPONSES)
async def keyword_recommend(request: KeywordRequest):
    ...
```

Embeddings 라우터도 동일.

- [ ] **Step 3: 나머지 3개 서비스 라우터 수정**

Review Writer, Operations, Validation 라우터 모두 동일한 패턴으로 `responses=ERROR_RESPONSES` 추가.

- [ ] **Step 4: 커밋**

```bash
git add ai-services/*/app/errors.py ai-services/*/app/routers/*.py
git commit -m "feat: add error response examples to FastAPI Swagger documentation"
```

---

### Task 10: README.md + CLAUDE.md 업데이트

에러 응답 규격, 페이지네이션 규격, 로깅, 보안 관련 내용을 문서에 추가한다.

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: README.md에 API 규격 섹션 추가**

"서비스 간 통신" 섹션 뒤에 다음 내용 추가:

```markdown
---

## API 공통 규격

### 에러 응답 포맷

모든 API는 오류 시 아래 JSON 포맷으로 응답합니다:

```json
{
  "timestamp": "2025-03-05T12:00:00Z",
  "path": "/api/menus/123",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "입력값 검증에 실패했습니다.",
  "details": { "name": "must not be blank" }
}
```

### 표준 에러 코드 (15종)

| HTTP | 코드 | 설명 |
|------|------|------|
| 400 | `BAD_REQUEST` | 요청 형식이 올바르지 않음 |
| 400 | `VALIDATION_FAILED` | 필드 유효성 검사 실패 |
| 400 | `INVALID_QUERY_PARAM` | 쿼리 파라미터 값이 잘못됨 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 잘못된 토큰 |
| 401 | `TOKEN_EXPIRED` | 토큰 만료 |
| 403 | `FORBIDDEN` | 접근 권한 없음 |
| 404 | `RESOURCE_NOT_FOUND` | 요청한 리소스가 존재하지 않음 |
| 404 | `USER_NOT_FOUND` | 사용자 ID가 존재하지 않음 |
| 409 | `DUPLICATE_RESOURCE` | 중복 데이터 존재 |
| 409 | `STATE_CONFLICT` | 리소스 상태 충돌 |
| 422 | `UNPROCESSABLE_ENTITY` | 처리할 수 없는 요청 내용 |
| 429 | `TOO_MANY_REQUESTS` | 요청 한도 초과 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
| 500 | `DATABASE_ERROR` | DB 연동 오류 |
| 500 | `UNKNOWN_ERROR` | 알 수 없는 오류 |

### 페이지네이션 규격

목록 조회 API는 다음 파라미터를 지원합니다:

| 파라미터 | 기본값 | 최대값 | 설명 |
|----------|--------|--------|------|
| `page` | 0 | - | 페이지 번호 (0-indexed) |
| `size` | 20 | 100 | 페이지당 항목 수 |
| `sort` | `createdAt,DESC` | - | 정렬 (field,ASC\|DESC) |

서비스별 검색/필터:

| 서비스 | 필터 파라미터 |
|--------|---------------|
| Menu | `category` (카테고리 ID), `keyword` (이름/설명/태그 검색) |
| Order | `status` (주문 상태), `dateFrom`/`dateTo` (날짜 범위) |
| Review | `menuId` (메뉴 ID), `rating` (별점 필터) |

응답 포맷:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 153,
  "totalPages": 8,
  "sort": "createdAt,DESC"
}
```

### 요청/응답 로깅

모든 서비스는 요청마다 아래 정보를 로깅합니다:
- 메서드, 경로, 상태코드, 지연시간 (ms)
- 에러 발생 시 스택트레이스 (민감정보 제외)

### 헬스체크

모든 서비스는 `GET /health` 엔드포인트를 제공합니다 (인증 불필요):

```json
{ "status": "UP", "version": "1.0.0", "buildTime": "2025-03-05T12:00:00Z" }
```

### API 문서

| 서비스 | Swagger UI |
|--------|------------|
| Auth | http://localhost:8081/api/auth/swagger-ui.html |
| Menu | http://localhost:8082/swagger-ui.html |
| Order | http://localhost:8083/swagger-ui.html |
| Review | http://localhost:8084/swagger-ui.html |
| AI Recommendation | http://localhost:8001/docs |
| AI Review Writer | http://localhost:8002/docs |
| AI Operations | http://localhost:8003/docs |
| AI Validation | http://localhost:8004/docs |
```

- [ ] **Step 2: CLAUDE.md에 에러/페이지네이션 규격 추가**

"공통 패턴" 섹션에 다음 추가:

```markdown
## 에러 응답 규격
- 모든 서비스 통일 JSON 포맷: `{timestamp, path, status, code, message, details}`
- 15종 표준 에러 코드 (ErrorCode enum / errors.py)
- Spring Boot: `GlobalExceptionHandler` (@RestControllerAdvice) + `BusinessException`
- FastAPI: `errors.py` (register_error_handlers) + `BusinessError`

## 페이지네이션 규격
- 0-indexed page, size (기본 20, 최대 100), sort (field,ASC|DESC)
- 응답: `{content, page, size, totalElements, totalPages, sort}`
- Menu: category, keyword 필터 / Order: status, dateFrom/dateTo 필터 / Review: menuId, rating 필터

## 로깅
- Spring Boot: `LoggingFilter` (OncePerRequestFilter) — 메서드, 경로, 상태코드, 지연시간
- FastAPI: `LoggingMiddleware` (BaseHTTPMiddleware) — 동일 포맷

## 헬스체크
- 모든 서비스: `GET /health` → `{status, version, buildTime}` (인증 불필요)

## 환경변수
- 각 서비스에 `.env.example` 제공
- 비밀키(JWT_SECRET, OPENAI_API_KEY)는 절대 코드에 하드코딩 금지
```

- [ ] **Step 3: 커밋**

```bash
git add README.md CLAUDE.md
git commit -m "docs: add API error/pagination/logging/health check specifications to README and CLAUDE.md"
```

---

## Execution Notes

- Task 1~2는 순차 (1이 패턴을 정하고 2가 복제)
- Task 3은 Task 2 이후 (ErrorResponse record 형태가 확정되어야 PageResponse도 맞출 수 있음)
- Task 4, 5는 독립적으로 병렬 가능
- Task 6, 7은 독립적으로 병렬 가능
- Task 8, 9는 Task 1~3 이후 (ErrorResponse가 확정되어야 Swagger 예시 작성 가능)
- Task 10은 마지막 (모든 변경이 확정된 후)
