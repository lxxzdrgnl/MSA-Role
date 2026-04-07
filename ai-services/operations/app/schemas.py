from pydantic import BaseModel


class CongestionResponse(BaseModel):
    level: str
    active_orders: int
    estimated_wait_minutes: int


class MenuCongestionResponse(BaseModel):
    menu_id: int
    menu_name: str
    estimated_minutes: int


class NewMenuSuggestRequest(BaseModel):
    order_history: list[dict]
    current_menus: list[dict]


class NewMenuResponse(BaseModel):
    suggestions: list[dict]
    analysis: str


class QualityAnalysisRequest(BaseModel):
    reviews: list[dict]
    order_stats: dict


class QualityResponse(BaseModel):
    overall_score: float
    strengths: list[str]
    weaknesses: list[str]
    recommendations: list[str]
