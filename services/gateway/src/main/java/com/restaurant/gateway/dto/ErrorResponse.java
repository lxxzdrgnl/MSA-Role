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
