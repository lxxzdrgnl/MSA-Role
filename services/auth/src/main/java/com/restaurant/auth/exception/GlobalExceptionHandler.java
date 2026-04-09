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
