import logging
import time as _time

from fastapi import FastAPI

_BUILD_TIME = _time.strftime("%Y-%m-%dT%H:%M:%SZ", _time.gmtime())

from app.errors import register_error_handlers
from app.middleware import LoggingMiddleware
from app.routers import operations

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Operations Service",
    description="AI 기반 혼잡도 분석/신메뉴 추천/품질 분석 서비스",
    version="1.0.0",
)

register_error_handlers(app)
app.add_middleware(LoggingMiddleware)

app.include_router(operations.router)


@app.get("/health")
async def health_check():
    return {"status": "UP", "version": "1.0.0", "buildTime": _BUILD_TIME}
