import logging

from fastapi import FastAPI

from app.routers import operations

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Operations Service",
    description="AI 기반 혼잡도 분석/신메뉴 추천/품질 분석 서비스",
    version="1.0.0",
)

app.include_router(operations.router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}
