from typing import List, Optional

from pydantic import BaseModel


class ChatRequest(BaseModel):
    message: str
    user_id: Optional[str] = None


class KeywordRequest(BaseModel):
    keywords: List[str]
    user_id: Optional[str] = None


class RecommendationResponse(BaseModel):
    menu_ids: List[int]
    reason: str
    keywords: List[str] = []


class EmbeddingSyncRequest(BaseModel):
    menu_id: int
    name: str
    description: Optional[str] = ""
    tags: Optional[str] = ""
    allergens: Optional[str] = ""
    spicy_level: Optional[int] = 0
    review_summary: Optional[str] = ""
    avg_rating: Optional[float] = 0.0
    order_count: Optional[int] = 0


class BulkSyncRequest(BaseModel):
    menus: List[EmbeddingSyncRequest]
