from fastapi import APIRouter

from app.errors import ERROR_RESPONSES
from app.schemas import (
    CongestionResponse,
    MenuCongestionResponse,
    NewMenuSuggestRequest,
    NewMenuResponse,
    QualityAnalysisRequest,
    QualityResponse,
)
from app.services import congestion_service, operations_service

router = APIRouter(prefix="/api/operations", tags=["operations"])


@router.get("/congestion", response_model=CongestionResponse, responses=ERROR_RESPONSES)
async def get_congestion():
    """Get current restaurant congestion status."""
    return await congestion_service.get_congestion()


@router.get("/congestion/menu/{menu_id}", response_model=MenuCongestionResponse, responses=ERROR_RESPONSES)
async def get_menu_congestion(menu_id: int):
    """Get estimated preparation time for a specific menu item."""
    return await congestion_service.get_menu_congestion(menu_id)


@router.post("/new-menu-suggest", response_model=NewMenuResponse, responses=ERROR_RESPONSES)
async def suggest_new_menu(request: NewMenuSuggestRequest):
    """Suggest new menu items based on order history."""
    return operations_service.suggest_new_menu(request)


@router.post("/quality-analysis", response_model=QualityResponse, responses=ERROR_RESPONSES)
async def analyze_quality(request: QualityAnalysisRequest):
    """Analyze overall service quality."""
    return operations_service.analyze_quality(request)
