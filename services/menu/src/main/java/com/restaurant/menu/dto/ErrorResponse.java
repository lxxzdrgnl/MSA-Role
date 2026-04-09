package com.restaurant.menu.dto;

import com.restaurant.menu.exception.ErrorCode;

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
