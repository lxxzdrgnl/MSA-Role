import logging

from fastapi import FastAPI

from app.routers import validation

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Validation Service",
    description="AI 입출력 검증 서비스",
    version="1.0.0",
)

app.include_router(validation.router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}
