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
