import logging

from fastapi import FastAPI

from app.routers import reviews

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Review Writer Service",
    description="AI 기반 리뷰 작성/분석/요약 서비스",
    version="1.0.0",
)

app.include_router(reviews.router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}
