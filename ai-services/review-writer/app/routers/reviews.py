from fastapi import APIRouter

from app.schemas import (
    GenerateRequest,
    GenerateResponse,
    AnalyzeRequest,
    AnalyzeResponse,
    SummarizeRequest,
    SummarizeResponse,
)
from app.services import review_writer_service

router = APIRouter(prefix="/reviews", tags=["reviews"])


@router.post("/generate", response_model=GenerateResponse)
async def generate_review(request: GenerateRequest):
    """Generate a review draft using AI."""
    return review_writer_service.generate_review(request)


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_reviews(request: AnalyzeRequest):
    """Analyze reviews for sentiment and keywords."""
    return review_writer_service.analyze_reviews(request)


@router.post("/summarize", response_model=SummarizeResponse)
async def summarize_reviews(request: SummarizeRequest):
    """Summarize reviews for menu embedding."""
    return review_writer_service.summarize_reviews(request)
