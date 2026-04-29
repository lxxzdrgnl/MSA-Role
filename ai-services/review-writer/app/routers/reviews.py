from fastapi import APIRouter

from app.errors import ERROR_RESPONSES
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


@router.post("/generate", response_model=GenerateResponse, responses=ERROR_RESPONSES)
async def generate_review(request: GenerateRequest):
    """AI 리뷰 초안 생성"""
    return review_writer_service.generate_review(request)


@router.post("/analyze", response_model=AnalyzeResponse, responses=ERROR_RESPONSES)
async def analyze_reviews(request: AnalyzeRequest):
    """리뷰 감정 분석 및 키워드 추출"""
    return review_writer_service.analyze_reviews(request)


@router.post("/summarize", response_model=SummarizeResponse, responses=ERROR_RESPONSES)
async def summarize_reviews(request: SummarizeRequest):
    """메뉴 임베딩용 리뷰 요약"""
    return review_writer_service.summarize_reviews(request)
