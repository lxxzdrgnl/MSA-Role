from fastapi import APIRouter

from app.errors import ERROR_RESPONSES
from app.schemas import ChatRequest, KeywordRequest, RecommendationResponse
from app.services import recommendation_service

router = APIRouter(prefix="/api/recommendations", tags=["recommendations"])


@router.post("/chat", response_model=RecommendationResponse, responses=ERROR_RESPONSES)
async def chat_recommend(request: ChatRequest):
    """Chat-based menu recommendation."""
    return recommendation_service.chat_recommend(request.message)


@router.post("/keyword", response_model=RecommendationResponse, responses=ERROR_RESPONSES)
async def keyword_recommend(request: KeywordRequest):
    """Keyword-based menu recommendation."""
    return recommendation_service.keyword_recommend(request.keywords)
