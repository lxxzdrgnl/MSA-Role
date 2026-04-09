import logging
import time as _time

from fastapi import FastAPI

_BUILD_TIME = _time.strftime("%Y-%m-%dT%H:%M:%SZ", _time.gmtime())

from app.errors import register_error_handlers
from app.middleware import LoggingMiddleware
from app.routers import reviews

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Review Writer Service",
    description="AI 기반 리뷰 작성/분석/요약 서비스",
    version="1.0.0",
)

register_error_handlers(app)
app.add_middleware(LoggingMiddleware)

app.include_router(reviews.router)


@app.get("/health")
async def health_check():
    return {"status": "UP", "version": "1.0.0", "buildTime": _BUILD_TIME}
