package com.restaurant.menu.exception;

public enum ErrorCode {
    BAD_REQUEST(400, "BAD_REQUEST", "잘못된 요청입니다."),
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "입력값 검증에 실패했습니다."),
    INVALID_QUERY_PARAM(400, "INVALID_QUERY_PARAM", "잘못된 쿼리 파라미터입니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(409, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    STATE_CONFLICT(409, "STATE_CONFLICT", "리소스 상태 충돌입니다."),
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "처리할 수 없는 요청입니다."),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "요청 한도를 초과했습니다."),
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
