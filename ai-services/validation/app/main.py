import logging
import time as _time

from fastapi import FastAPI

_BUILD_TIME = _time.strftime("%Y-%m-%dT%H:%M:%SZ", _time.gmtime())

from app.errors import register_error_handlers
from app.middleware import LoggingMiddleware
from app.routers import validation

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Validation Service",
    description="AI 입출력 검증 서비스",
    version="1.0.0",
)

register_error_handlers(app)
app.add_middleware(LoggingMiddleware)

app.include_router(validation.router)


@app.get("/health")
async def health_check():
    return {"status": "UP", "version": "1.0.0", "buildTime": _BUILD_TIME}
